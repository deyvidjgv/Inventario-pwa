package com.deyvidjgv.inventario.domain.port;

import com.deyvidjgv.inventario.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(long id);
    List<Product> findAll();
    List<Product> findActive();
    void deleteAll();
}
