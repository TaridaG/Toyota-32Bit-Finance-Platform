package com.company.logconsumer.shared.web;

/**
 * İstenen kaynak bulunamadığında fırlatılan runtime exception.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
