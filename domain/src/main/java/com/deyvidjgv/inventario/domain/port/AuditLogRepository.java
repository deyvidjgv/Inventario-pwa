package com.deyvidjgv.inventario.domain.port;

import com.deyvidjgv.inventario.domain.model.AuditLog;

import java.util.List;

public interface AuditLogRepository {
    AuditLog save(AuditLog log);
    List<AuditLog> findByEntity(String entity, long entityId);
    List<AuditLog> findAll();
    void deleteAll();
}
