package com.deyvidjgv.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.deyvidjgv.inventario.data.local.entity.ExpenseEntity;

import java.time.Instant;
import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ExpenseEntity entity);

    @Query("DELETE FROM expenses WHERE id = :id")
    void delete(long id);

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    ExpenseEntity findById(long id);

    @Query("SELECT * FROM expenses WHERE jornadaId = :jornadaId ORDER BY createdAt ASC")
    List<ExpenseEntity> findByJornadaId(long jornadaId);

    @Query("SELECT * FROM expenses WHERE createdAt >= :from AND createdAt <= :to ORDER BY createdAt ASC")
    List<ExpenseEntity> findByPeriod(Instant from, Instant to);

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    List<ExpenseEntity> findAll();

    @Query("DELETE FROM expenses")
    void deleteAll();
}
