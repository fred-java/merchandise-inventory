package com.mateus.merchandiseinventory.exception;

import com.mateus.merchandiseinventory.filter.RequestTracingFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException ex) {
        return reject(HttpStatus.NOT_FOUND, "Produto não encontrado", ex.getMessage(), ex);
    }

    @ExceptionHandler(ProductVersionConflictException.class)
    public ProblemDetail handleVersionConflict(ProductVersionConflictException ex) {
        return reject(HttpStatus.CONFLICT, "Conflito de versão", ex.getMessage(), ex);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentModification(OptimisticLockingFailureException ex) {
        return reject(HttpStatus.CONFLICT, "Conflito de versão",
                "O produto foi alterado por outra requisição. Consulte-o novamente e tente de novo.", ex);
    }

    @ExceptionHandler(InvalidSortPropertyException.class)
    public ProblemDetail handleInvalidSort(InvalidSortPropertyException ex) {
        return reject(HttpStatus.BAD_REQUEST, "Ordenação inválida", ex.getMessage(), ex);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Informe o requestId ao suporte para análise.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail body = ex.getBody();
        body.setTitle("Dados inválidos");
        body.setDetail("Um ou mais campos são inválidos");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);

        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            addRequestId(problem);
            Object errors = problem.getProperties() == null ? null : problem.getProperties().get("errors");
            log.warn("Requisição rejeitada com status {}: {}{}", statusCode.value(), problem.getDetail(),
                    errors == null ? "" : " " + errors);
        }
        return response;
    }

    private static ProblemDetail reject(HttpStatus status, String title, String detail, Exception ex) {
        log.warn("Requisição rejeitada com status {}: {}", status.value(), ex.getMessage());
        return problem(status, title, detail);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        addRequestId(problem);
        return problem;
    }

    private static void addRequestId(ProblemDetail problem) {
        String requestId = MDC.get(RequestTracingFilter.REQUEST_ID_KEY);
        if (requestId != null) {
            problem.setProperty(RequestTracingFilter.REQUEST_ID_KEY, requestId);
        }
    }
}
