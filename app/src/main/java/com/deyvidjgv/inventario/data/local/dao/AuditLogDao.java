package com.deyvidjgv.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.deyvidjgv.inventario.data.local.entity.AuditLogEntity;

import java.util.List;

@Dao
public interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(AuditLogEntity entity);

    @Query("SELECT * FROM audit_logs WHERE entity = :entity AND entityId = :entityId ORDER BY createdAt DESC")
    List<AuditLogEntity> findByEntity(String entity, long entityId);

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC")
    List<AuditLogEntity> findAll();

    @Query("DELETE FROM audit_logs")
    void deleteAll();
}
