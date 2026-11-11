package com.mateus.merchandiseinventory.exception;

public class ProductVersionConflictException extends RuntimeException {

    public ProductVersionConflictException(Long id, Long expectedVersion, Long currentVersion) {
        super("O produto %d foi alterado por outra requisição (versão enviada %d, versão atual %d). Consulte-o novamente e tente de novo."
                .formatted(id, expectedVersion, currentVersion));
    }
}
