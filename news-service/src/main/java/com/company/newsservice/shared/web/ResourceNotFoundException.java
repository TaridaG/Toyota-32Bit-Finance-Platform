package com.company.newsservice.shared.web;

/**
 * İstenen kaynak bulunamadığında fırlatılan runtime exception.
 */
public class ResourceNotFoundException extends RuntimeException {

    /** Kullanıcıya veya log'a yansıyacak mesajla exception oluşturur. */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}