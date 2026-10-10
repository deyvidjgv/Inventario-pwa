package com.deyvidjgv.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.deyvidjgv.inventario.data.local.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CategoryEntity entity);

    @Update
    void update(CategoryEntity entity);

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    CategoryEntity findById(long id);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<CategoryEntity> findAll();

    @Query("DELETE FROM categories")
    void deleteAll();
}
