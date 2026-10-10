package com.deyvidjgv.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.deyvidjgv.inventario.data.local.entity.StockAdjustmentEntity;

import java.util.List;

@Dao
public interface StockAdjustmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(StockAdjustmentEntity entity);

    @Query("SELECT * FROM stock_adjustments WHERE jornadaId = :jornadaId ORDER BY createdAt ASC")
    List<StockAdjustmentEntity> findByJornadaId(long jornadaId);

    @Query("SELECT * FROM stock_adjustments ORDER BY createdAt DESC")
    List<StockAdjustmentEntity> findAll();

    @Query("DELETE FROM stock_adjustments")
    void deleteAll();
}
