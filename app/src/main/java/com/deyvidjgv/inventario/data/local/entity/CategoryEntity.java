package com.deyvidjgv.inventario.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.deyvidjgv.inventario.domain.model.Category;

@Entity(tableName = "categories")
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    public String name;

    public CategoryEntity() {}

    public CategoryEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static CategoryEntity fromDomain(Category category) {
        return new CategoryEntity(category.getId(), category.getName());
    }

    public Category toDomain() {
        return new Category(id, name);
    }
}
