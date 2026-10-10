package com.deyvidjgv.inventario.domain.model;

import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;

import java.util.Objects;

public class Product {
    private final Long id;
    private final String name;
    private final long categoryId;
    private final long salePrice;
    private final boolean tracksStock;
    private final boolean active;

    public Product(Long id, String name, long categoryId, long salePrice, boolean tracksStock, boolean active) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío");
        }
        if (salePrice < 0) {
            throw new DomainException(ErrorCode.INVALID_SALE_PRICE, "El precio de venta no puede ser negativo");
        }
        this.id = id;
        this.name = name.trim();
        this.categoryId = categoryId;
        this.salePrice = salePrice;
        this.tracksStock = tracksStock;
        this.active = active;
    }

    public Product(String name, long categoryId, long salePrice, boolean tracksStock) {
        this(null, name, categoryId, salePrice, tracksStock, true);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public long getSalePrice() {
        return salePrice;
    }

    public boolean isTracksStock() {
        return tracksStock;
    }

    public boolean isActive() {
        return active;
    }

    public Product withId(Long newId) {
        return new Product(newId, this.name, this.categoryId, this.salePrice, this.tracksStock, this.active);
    }

    public Product withActive(boolean newActive) {
        return new Product(this.id, this.name, this.categoryId, this.salePrice, this.tracksStock, newActive);
    }

    public Product withSalePrice(long newSalePrice) {
        return new Product(this.id, this.name, this.categoryId, newSalePrice, this.tracksStock, this.active);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return categoryId == product.categoryId &&
                salePrice == product.salePrice &&
                tracksStock == product.tracksStock &&
                active == product.active &&
                Objects.equals(id, product.id) &&
                Objects.equals(name, product.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, categoryId, salePrice, tracksStock, active);
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', categoryId=" + categoryId +
                ", salePrice=" + salePrice + ", tracksStock=" + tracksStock + ", active=" + active + "}";
    }
}
