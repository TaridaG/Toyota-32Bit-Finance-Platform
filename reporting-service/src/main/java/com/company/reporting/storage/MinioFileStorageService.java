package com.company.reporting.storage;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${storage.bucket}")
    private String bucket;

    @Override
    public String upload(
            String fileName,
            byte[] content,
            String contentType
    ) {

        try {

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType(contentType)
                            .build()
            );

            return fileName;

        } catch (Exception e) {
            throw new IllegalStateException("Minio upload failed", e);
        }
    }

    @Override
    public byte[] download(String fileName) {

        try {

            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .build()
            );

            return response.readAllBytes();

        } catch (Exception e) {
            throw new IllegalStateException("Minio download failed", e);
        }
    }
}