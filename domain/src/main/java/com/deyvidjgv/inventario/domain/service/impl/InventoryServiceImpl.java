package com.deyvidjgv.inventario.domain.service.impl;

import com.deyvidjgv.inventario.domain.dto.ProductStock;
import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;
import com.deyvidjgv.inventario.domain.model.Category;
import com.deyvidjgv.inventario.domain.model.Product;
import com.deyvidjgv.inventario.domain.model.StockLot;
import com.deyvidjgv.inventario.domain.port.CategoryRepository;
import com.deyvidjgv.inventario.domain.port.ProductRepository;
import com.deyvidjgv.inventario.domain.port.StockLotRepository;
import com.deyvidjgv.inventario.domain.service.InventoryService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InventoryServiceImpl implements InventoryService {
    private final ProductRepository productRepository;
    private final StockLotRepository stockLotRepository;
    private final CategoryRepository categoryRepository;

    public InventoryServiceImpl(ProductRepository productRepository,
                                StockLotRepository stockLotRepository,
                                CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.stockLotRepository = stockLotRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Product createProduct(String name, long categoryId, long salePrice, boolean tracksStock) {
        if (!categoryRepository.findById(categoryId).isPresent()) {
            throw new DomainException(ErrorCode.CATEGORY_NOT_FOUND, "La categoría especificada no existe: " + categoryId);
        }
        Product product = new Product(name, categoryId, salePrice, tracksStock);
        return productRepository.save(product);
    }

    @Override
    public void updateProduct(Product product) {
        if (product.getId() == null || !productRepository.findById(product.getId()).isPresent()) {
            throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "El producto a actualizar no existe: " + product.getId());
        }
        productRepository.save(product);
    }

    @Override
    public void archiveProduct(long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "Producto no encontrado: " + productId));
        Product archived = product.withActive(false);
        productRepository.save(archived);
    }

    @Override
    public void unarchiveProduct(long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "Producto no encontrado: " + productId));
        Product unarchived = product.withActive(true);
        productRepository.save(unarchived);
    }

    @Override
    public StockLot receiveStock(long productId, int quantity, long unitCost, Instant at, String note) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "Producto no encontrado: " + productId));

        if (!product.isActive()) {
            throw new DomainException(ErrorCode.PRODUCT_INACTIVE, "No se puede ingresar stock a un producto archivado");
        }
        if (!product.isTracksStock()) {
            throw new DomainException(ErrorCode.PRODUCT_INACTIVE, "No se puede ingresar stock a un producto de servicio (sin control de inventario)");
        }
        if (quantity <= 0) {
            throw new DomainException(ErrorCode.INVALID_QUANTITY, "La cantidad a ingresar debe ser mayor a 0");
        }
        if (unitCost < 0) {
            throw new DomainException(ErrorCode.INVALID_UNIT_COST, "El precio de compra unitario es obligatorio y no puede ser negativo");
        }

        Instant receivedAt = at != null ? at : Instant.now();
        StockLot lot = new StockLot(productId, quantity, unitCost, receivedAt, note);
        return stockLotRepository.save(lot);
    }

    @Override
    public List<ProductStock> listStock() {
        return listStock(false);
    }

    @Override
    public List<ProductStock> listStock(boolean includeArchived) {
        List<Product> products = includeArchived ? productRepository.findAll() : productRepository.findActive();
        Map<Long, String> categoriesById = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        List<ProductStock> result = new ArrayList<>();
        for (Product product : products) {
            List<StockLot> activeLots = stockLotRepository.findActiveByProductId(product.getId());
            int totalStock = activeLots.stream().mapToInt(StockLot::getQuantityRemaining).sum();
            String categoryName = categoriesById.getOrDefault(product.getCategoryId(), "General");
            result.add(new ProductStock(product, categoryName, totalStock, activeLots));
        }
        return result;
    }

    @Override
    public Category createCategory(String name) {
        Category category = new Category(name);
        return categoryRepository.save(category);
    }

    @Override
    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public java.util.Optional<Product> getProduct(long productId) {
        return productRepository.findById(productId);
    }
}
