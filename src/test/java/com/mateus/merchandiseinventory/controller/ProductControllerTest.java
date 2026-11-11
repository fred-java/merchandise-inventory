package com.mateus.merchandiseinventory.controller;

import com.mateus.merchandiseinventory.assembler.ProductModelAssembler;
import com.mateus.merchandiseinventory.dto.CreateProductRequest;
import com.mateus.merchandiseinventory.dto.ProductResponse;
import com.mateus.merchandiseinventory.dto.UpdateProductRequest;
import com.mateus.merchandiseinventory.exception.InvalidSortPropertyException;
import com.mateus.merchandiseinventory.exception.ProductNotFoundException;
import com.mateus.merchandiseinventory.exception.ProductVersionConflictException;
import com.mateus.merchandiseinventory.filter.RequestTracingFilter;
import com.mateus.merchandiseinventory.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(ProductModelAssembler.class)
class ProductControllerTest {

    private static final String PRODUCT_JSON = """
            {"name": "Arroz Tipo 1 5kg", "description": "Longo fino", "price": 27.90, "stockQuantity": 150}
            """;

    private static final ProductResponse PRODUCT = new ProductResponse(
            1L, "Arroz Tipo 1 5kg", "Longo fino", new BigDecimal("27.90"), 150, 0L,
            Instant.parse("2026-11-10T12:00:00Z"), Instant.parse("2026-11-10T12:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(productService.create(any(CreateProductRequest.class))).thenReturn(PRODUCT);

        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(PRODUCT_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/products/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/products/1"))
                .andExpect(jsonPath("$._links.update.href").value("http://localhost/api/v1/products/1"))
                .andExpect(jsonPath("$._links.delete.href").value("http://localhost/api/v1/products/1"))
                .andExpect(jsonPath("$._links.products.href").exists());
    }

    @Test
    void createReturns400WithTheInvalidFields() throws Exception {
        String invalid = """
                {"name": " ", "price": -1, "stockQuantity": -5}
                """;

        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Um ou mais campos são inválidos"))
                .andExpect(jsonPath("$.errors.name").value("não deve estar em branco"))
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stockQuantity").exists())
                .andExpect(jsonPath("$.requestId").exists());

        verifyNoInteractions(productService);
    }

    @Test
    void createReturns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void findByIdReturns400ForANonNumericId() throws Exception {
        mockMvc.perform(get("/api/v1/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.detail").value("O valor \"abc\" não é válido para o parâmetro \"id\"."));
    }

    @Test
    void unexpectedErrorsReturn500WithoutLeakingDetails() throws Exception {
        when(productService.findById(1L)).thenThrow(new IllegalStateException("connection pool exhausted"));

        mockMvc.perform(get("/api/v1/products/1").header(RequestTracingFilter.REQUEST_ID_HEADER, "abc-123"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(RequestTracingFilter.REQUEST_ID_HEADER, "abc-123"))
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Informe o requestId ao suporte para análise."))
                .andExpect(jsonPath("$.requestId").value("abc-123"));
    }

    @Test
    void findAllUsesTheRequestedPage() throws Exception {
        when(productService.findAll(any(Pageable.class))).thenAnswer(invocation ->
                new PageImpl<>(List.of(PRODUCT), invocation.<Pageable>getArgument(0), 11));

        mockMvc.perform(get("/api/v1/products").param("page", "1").param("size", "5").param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.products[0].id").value(1))
                .andExpect(jsonPath("$._embedded.products[0]._links.self.href").value("http://localhost/api/v1/products/1"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(11))
                .andExpect(jsonPath("$.page.totalPages").value(3))
                .andExpect(jsonPath("$._links.first.href").value(containsString("page=0")))
                .andExpect(jsonPath("$._links.prev.href").value(containsString("page=0")))
                .andExpect(jsonPath("$._links.next.href").value(containsString("page=2")))
                .andExpect(jsonPath("$._links.last.href").value(containsString("page=2")))
                .andExpect(jsonPath("$._links.next.href").value(containsString("sort=price,desc")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAllSortsByNameByDefault() throws Exception {
        when(productService.findAll(any(Pageable.class))).thenReturn(Page.empty(PageRequest.of(0, 20)));

        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().getOrderFor("name")).isNotNull();
    }

    @Test
    void findAllReturns400ForAnUnknownSortProperty() throws Exception {
        when(productService.findAll(any(Pageable.class)))
                .thenThrow(new InvalidSortPropertyException("password", List.of("name")));

        mockMvc.perform(get("/api/v1/products").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ordenação inválida"));
    }

    @Test
    void findByIdReturnsTheProduct() throws Exception {
        when(productService.findById(1L)).thenReturn(PRODUCT);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arroz Tipo 1 5kg"))
                .andExpect(jsonPath("$.price").value(27.90));
    }

    @Test
    void findByIdReturns404AsProblemDetail() throws Exception {
        when(productService.findById(7L)).thenThrow(new ProductNotFoundException(7L));

        mockMvc.perform(get("/api/v1/products/7"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.detail").value("Produto 7 não encontrado"));
    }

    @Test
    void updateReturnsTheUpdatedProduct() throws Exception {
        when(productService.update(eq(1L), any(UpdateProductRequest.class))).thenReturn(PRODUCT);

        mockMvc.perform(put("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Arroz Tipo 1 5kg", "price": 27.90, "stockQuantity": 150, "version": 0}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateRequiresTheVersion() throws Exception {
        mockMvc.perform(put("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON).content(PRODUCT_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.version").exists());
    }

    @Test
    void updateReturns409OnAStaleVersion() throws Exception {
        when(productService.update(eq(1L), any(UpdateProductRequest.class)))
                .thenThrow(new ProductVersionConflictException(1L, 0L, 2L));

        mockMvc.perform(put("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Arroz Tipo 1 5kg", "price": 27.90, "stockQuantity": 150, "version": 0}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de versão"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1")).andExpect(status().isNoContent());

        verify(productService).delete(1L);
    }

    @Test
    void deleteReturns409WhenTheRowChangedConcurrently() throws Exception {
        doThrow(new ObjectOptimisticLockingFailureException("Product", 1L)).when(productService).delete(1L);

        mockMvc.perform(delete("/api/v1/products/1")).andExpect(status().isConflict());
    }
}
