package com.deyvidjgv.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.deyvidjgv.inventario.data.local.entity.StockLotEntity;

import java.util.List;

@Dao
public interface StockLotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(StockLotEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<StockLotEntity> entities);

    @Update
    void update(StockLotEntity entity);

    @Update
    void updateAll(List<StockLotEntity> entities);

    @Query("SELECT * FROM stock_lots WHERE id = :id LIMIT 1")
    StockLotEntity findById(long id);

    @Query("SELECT * FROM stock_lots WHERE productId = :productId AND quantityRemaining > 0 ORDER BY receivedAt ASC, id ASC")
    List<StockLotEntity> findActiveByProductId(long productId);

    @Query("SELECT * FROM stock_lots WHERE productId = :productId ORDER BY receivedAt ASC, id ASC")
    List<StockLotEntity> findAllByProductId(long productId);

    @Query("SELECT * FROM stock_lots WHERE productId = :productId ORDER BY receivedAt DESC, id DESC LIMIT 1")
    StockLotEntity findNewestByProductId(long productId);

    @Query("SELECT * FROM stock_lots ORDER BY receivedAt ASC, id ASC")
    List<StockLotEntity> findAll();

    @Query("DELETE FROM stock_lots")
    void deleteAll();
}
