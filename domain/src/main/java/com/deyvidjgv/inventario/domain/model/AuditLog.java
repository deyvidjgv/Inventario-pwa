package com.deyvidjgv.inventario.domain.model;

import java.time.Instant;
import java.util.Objects;

public class AuditLog {
    private final Long id;
    private final String entity;
    private final long entityId;
    private final String action;
    private final String beforeJson;
    private final String afterJson;
    private final Instant createdAt;

    public AuditLog(Long id, String entity, long entityId, String action, String beforeJson, String afterJson, Instant createdAt) {
        this.id = id;
        this.entity = entity;
        this.entityId = entityId;
        this.action = action;
        this.beforeJson = beforeJson != null ? beforeJson : "";
        this.afterJson = afterJson != null ? afterJson : "";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public AuditLog(String entity, long entityId, String action, String beforeJson, String afterJson, Instant createdAt) {
        this(null, entity, entityId, action, beforeJson, afterJson, createdAt);
    }

    public Long getId() {
        return id;
    }

    public String getEntity() {
        return entity;
    }

    public long getEntityId() {
        return entityId;
    }

    public String getAction() {
        return action;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public String getAfterJson() {
        return afterJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AuditLog withId(Long newId) {
        return new AuditLog(newId, entity, entityId, action, beforeJson, afterJson, createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return entityId == auditLog.entityId &&
                Objects.equals(id, auditLog.id) &&
                Objects.equals(entity, auditLog.entity) &&
                Objects.equals(action, auditLog.action) &&
                Objects.equals(beforeJson, auditLog.beforeJson) &&
                Objects.equals(afterJson, auditLog.afterJson) &&
                Objects.equals(createdAt, auditLog.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, entity, entityId, action, beforeJson, afterJson, createdAt);
    }

    @Override
    public String toString() {
        return "AuditLog{id=" + id + ", entity='" + entity + "', entityId=" + entityId +
                ", action='" + action + "', createdAt=" + createdAt + "}";
    }
}
