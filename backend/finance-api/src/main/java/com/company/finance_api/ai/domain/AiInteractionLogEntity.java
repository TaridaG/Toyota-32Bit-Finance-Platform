package com.company.finance_api.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_interaction_log")
public class AiInteractionLogEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "task_type", nullable = false, length = 80)
    private String taskType;

    @Column(length = 10)
    private String language;

    @Column(name = "source_language", length = 10)
    private String sourceLanguage;

    @Column(name = "target_language", length = 10)
    private String targetLanguage;

    @Column(length = 100)
    private String model;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "input_hash", length = 128)
    private String inputHash;

    @Column(name = "response_chars")
    private Integer responseChars;

    protected AiInteractionLogEntity() {
    }

    public static AiInteractionLogEntity create() {
        return new AiInteractionLogEntity();
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Integer getResponseChars() {
        return responseChars;
    }

    public void setResponseChars(Integer responseChars) {
        this.responseChars = responseChars;
    }

    public String getInputHash() {
        return inputHash;
    }

    public void setInputHash(String inputHash) {
        this.inputHash = inputHash;
    }
}
