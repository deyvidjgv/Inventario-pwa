package com.deyvidjgv.inventario.data.repository;

import com.deyvidjgv.inventario.data.local.dao.ProductDao;
import com.deyvidjgv.inventario.data.local.entity.ProductEntity;
import com.deyvidjgv.inventario.domain.model.Product;
import com.deyvidjgv.inventario.domain.port.ProductRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomProductRepository implements ProductRepository {
    private final ProductDao productDao;

    public RoomProductRepository(ProductDao productDao) {
        this.productDao = productDao;
    }

    @Override
    public Product save(Product product) {
        ProductEntity entity = ProductEntity.fromDomain(product);
        long id = productDao.insert(entity);
        return product.withId(id);
    }

    @Override
    public Optional<Product> findById(long id) {
        ProductEntity entity = productDao.findById(id);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<Product> findAll() {
        List<ProductEntity> entities = productDao.findAll();
        List<Product> list = new ArrayList<>();
        for (ProductEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<Product> findActive() {
        List<ProductEntity> entities = productDao.findActive();
        List<Product> list = new ArrayList<>();
        for (ProductEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public void deleteAll() {
        productDao.deleteAll();
    }
}
