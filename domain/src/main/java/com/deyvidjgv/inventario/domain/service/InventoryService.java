package com.deyvidjgv.inventario.domain.service;

import com.deyvidjgv.inventario.domain.dto.ProductStock;
import com.deyvidjgv.inventario.domain.model.Product;
import com.deyvidjgv.inventario.domain.model.StockLot;

import java.time.Instant;
import java.util.List;

public interface InventoryService {
    Product createProduct(String name, long categoryId, long salePrice, boolean tracksStock);
    void updateProduct(Product product);
    void archiveProduct(long productId);
    StockLot receiveStock(long productId, int quantity, long unitCost, Instant at, String note);
    List<ProductStock> listStock();
}
