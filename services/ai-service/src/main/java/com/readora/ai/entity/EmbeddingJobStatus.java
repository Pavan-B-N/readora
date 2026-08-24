package com.readora.ai.entity;

/** QUEUED -> RUNNING -> COMPLETED | FAILED. Only one job may be QUEUED or RUNNING at a time. */
public enum EmbeddingJobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED
}
