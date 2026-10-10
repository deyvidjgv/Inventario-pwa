package com.deyvidjgv.inventario.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.deyvidjgv.inventario.domain.model.AuditLog;

import java.time.Instant;

@Entity(
    tableName = "audit_logs",
    indices = {@Index("entity"), @Index("entityId"), @Index("createdAt")}
)
public class AuditLogEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    public String entity;
    public long entityId;
    public String action;
    public String beforeJson;
    public String afterJson;
    public Instant createdAt;

    public AuditLogEntity() {}

    @Ignore
    public AuditLogEntity(Long id, String entity, long entityId, String action, String beforeJson, String afterJson, Instant createdAt) {
        this.id = id;
        this.entity = entity;
        this.entityId = entityId;
        this.action = action;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.createdAt = createdAt;
    }

    public static AuditLogEntity fromDomain(AuditLog log) {
        return new AuditLogEntity(
            log.getId(),
            log.getEntity(),
            log.getEntityId(),
            log.getAction(),
            log.getBeforeJson(),
            log.getAfterJson(),
            log.getCreatedAt()
        );
    }

    public AuditLog toDomain() {
        return new AuditLog(id, entity, entityId, action, beforeJson, afterJson, createdAt);
    }
}
