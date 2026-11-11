package com.mateus.merchandiseinventory.exception;

import java.util.Collection;
import java.util.TreeSet;

public class InvalidSortPropertyException extends RuntimeException {

    public InvalidSortPropertyException(String property, Collection<String> allowed) {
        super("Não é possível ordenar por '%s'. Campos permitidos: %s".formatted(property, new TreeSet<>(allowed)));
    }
}