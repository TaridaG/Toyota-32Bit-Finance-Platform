package com.company.reporting.storage;

public interface FileStorageService {

    String upload(
            String fileName,
            byte[] content,
            String contentType
    );

    byte[] download(String fileName);
}