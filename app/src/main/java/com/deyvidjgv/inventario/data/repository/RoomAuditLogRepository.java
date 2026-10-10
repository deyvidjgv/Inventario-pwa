package com.deyvidjgv.inventario.data.repository;

import com.deyvidjgv.inventario.data.local.dao.AuditLogDao;
import com.deyvidjgv.inventario.data.local.entity.AuditLogEntity;
import com.deyvidjgv.inventario.domain.model.AuditLog;
import com.deyvidjgv.inventario.domain.port.AuditLogRepository;

import java.util.ArrayList;
import java.util.List;

public class RoomAuditLogRepository implements AuditLogRepository {
    private final AuditLogDao auditLogDao;

    public RoomAuditLogRepository(AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    @Override
    public AuditLog save(AuditLog log) {
        AuditLogEntity entity = AuditLogEntity.fromDomain(log);
        long id = auditLogDao.insert(entity);
        return log.withId(id);
    }

    @Override
    public List<AuditLog> findByEntity(String entity, long entityId) {
        List<AuditLogEntity> entities = auditLogDao.findByEntity(entity, entityId);
        List<AuditLog> list = new ArrayList<>();
        for (AuditLogEntity e : entities) {
            list.add(e.toDomain());
        }
        return list;
    }

    @Override
    public List<AuditLog> findAll() {
        List<AuditLogEntity> entities = auditLogDao.findAll();
        List<AuditLog> list = new ArrayList<>();
        for (AuditLogEntity e : entities) {
            list.add(e.toDomain());
        }
        return list;
    }

    @Override
    public void deleteAll() {
        auditLogDao.deleteAll();
    }
}
