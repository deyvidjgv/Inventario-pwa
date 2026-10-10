package com.deyvidjgv.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.deyvidjgv.inventario.data.local.entity.JornadaEntity;

import java.util.List;

@Dao
public interface JornadaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(JornadaEntity entity);

    @Update
    void update(JornadaEntity entity);

    @Query("SELECT * FROM jornadas WHERE id = :id LIMIT 1")
    JornadaEntity findById(long id);

    @Query("SELECT * FROM jornadas WHERE closedAt IS NULL LIMIT 1")
    JornadaEntity findOpen();

    @Query("SELECT * FROM jornadas WHERE closedAt IS NOT NULL ORDER BY closedAt DESC LIMIT 1")
    JornadaEntity findLastClosed();

    @Query("SELECT * FROM jornadas ORDER BY openedAt DESC")
    List<JornadaEntity> findAll();

    @Query("DELETE FROM jornadas")
    void deleteAll();
}
