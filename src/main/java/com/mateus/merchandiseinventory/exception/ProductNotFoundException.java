package com.mateus.merchandiseinventory.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Produto %d não encontrado".formatted(id));
    }
}
