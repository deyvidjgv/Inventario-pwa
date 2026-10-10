package com.deyvidjgv.inventario.domain.dto;

import com.deyvidjgv.inventario.domain.model.Product;
import com.deyvidjgv.inventario.domain.model.StockLot;

import java.util.Collections;
import java.util.List;

public class ProductStock {
    private final Product product;
    private final String categoryName;
    private final int currentStock;
    private final List<StockLot> lots;

    public ProductStock(Product product, String categoryName, int currentStock, List<StockLot> lots) {
        this.product = product;
        this.categoryName = categoryName != null ? categoryName : "";
        this.currentStock = currentStock;
        this.lots = lots != null ? Collections.unmodifiableList(lots) : Collections.emptyList();
    }

    public Product getProduct() {
        return product;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public List<StockLot> getLots() {
        return lots;
    }
}
