package com.deyvidjgv.inventario.domain.port;

import com.deyvidjgv.inventario.domain.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(long id);
    List<Category> findAll();
    void deleteAll();
}
