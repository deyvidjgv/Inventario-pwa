package com.deyvidjgv.inventario.data.repository;

import com.deyvidjgv.inventario.data.local.dao.CategoryDao;
import com.deyvidjgv.inventario.data.local.entity.CategoryEntity;
import com.deyvidjgv.inventario.domain.model.Category;
import com.deyvidjgv.inventario.domain.port.CategoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomCategoryRepository implements CategoryRepository {
    private final CategoryDao categoryDao;

    public RoomCategoryRepository(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    @Override
    public Category save(Category category) {
        CategoryEntity entity = CategoryEntity.fromDomain(category);
        if (category.getId() != null) {
            categoryDao.update(entity);
            return category;
        } else {
            long id = categoryDao.insert(entity);
            return category.withId(id);
        }
    }

    @Override
    public Optional<Category> findById(long id) {
        CategoryEntity entity = categoryDao.findById(id);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<Category> findAll() {
        List<CategoryEntity> entities = categoryDao.findAll();
        List<Category> list = new ArrayList<>();
        for (CategoryEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public void deleteAll() {
        categoryDao.deleteAll();
    }
}
