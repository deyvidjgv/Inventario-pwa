package com.deyvidjgv.inventario.domain.model;

import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;

import java.time.Instant;
import java.util.Objects;

public class Sale {
    private final Long id;
    private final long jornadaId;
    private final long productId;
    private final int quantity;
    private final long unitPrice;
    private final Instant createdAt;
    private final boolean voided;

    public Sale(Long id, long jornadaId, long productId, int quantity, long unitPrice, Instant createdAt, boolean voided) {
        if (quantity <= 0) {
            throw new DomainException(ErrorCode.INVALID_QUANTITY, "La cantidad a vender debe ser mayor a 0");
        }
        if (unitPrice < 0) {
            throw new DomainException(ErrorCode.INVALID_SALE_PRICE, "El precio unitario de venta no puede ser negativo");
        }
        this.id = id;
        this.jornadaId = jornadaId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.voided = voided;
    }

    public Sale(long jornadaId, long productId, int quantity, long unitPrice, Instant createdAt) {
        this(null, jornadaId, productId, quantity, unitPrice, createdAt, false);
    }

    public Long getId() {
        return id;
    }

    public long getJornadaId() {
        return jornadaId;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isVoided() {
        return voided;
    }

    public long getTotal() {
        return unitPrice * quantity;
    }

    public Sale withId(Long newId) {
        return new Sale(newId, jornadaId, productId, quantity, unitPrice, createdAt, voided);
    }

    public Sale markVoided() {
        if (this.voided) {
            throw new DomainException(ErrorCode.SALE_ALREADY_VOIDED, "La venta ya está anulada");
        }
        return new Sale(this.id, this.jornadaId, this.productId, this.quantity, this.unitPrice, this.createdAt, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sale sale = (Sale) o;
        return jornadaId == sale.jornadaId &&
                productId == sale.productId &&
                quantity == sale.quantity &&
                unitPrice == sale.unitPrice &&
                voided == sale.voided &&
                Objects.equals(id, sale.id) &&
                Objects.equals(createdAt, sale.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, jornadaId, productId, quantity, unitPrice, createdAt, voided);
    }

    @Override
    public String toString() {
        return "Sale{id=" + id + ", jornadaId=" + jornadaId + ", productId=" + productId +
                ", quantity=" + quantity + ", unitPrice=" + unitPrice + ", voided=" + voided + "}";
    }
}
