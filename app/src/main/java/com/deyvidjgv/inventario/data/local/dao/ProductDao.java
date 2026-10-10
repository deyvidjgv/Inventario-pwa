package com.deyvidjgv.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.deyvidjgv.inventario.data.local.entity.ProductEntity;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ProductEntity entity);

    @Update
    void update(ProductEntity entity);

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    ProductEntity findById(long id);

    @Query("SELECT * FROM products ORDER BY name ASC")
    List<ProductEntity> findAll();

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name ASC")
    List<ProductEntity> findActive();

    @Query("DELETE FROM products")
    void deleteAll();
}
