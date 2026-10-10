package com.deyvidjgv.inventario.domain.model;

import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;

import java.time.Instant;
import java.util.Objects;

public class StockLot {
    private final Long id;
    private final long productId;
    private final Instant receivedAt;
    private final int quantityIn;
    private final int quantityRemaining;
    private final long unitCost;
    private final String note;

    public StockLot(Long id, long productId, Instant receivedAt, int quantityIn, int quantityRemaining, long unitCost, String note) {
        if (quantityIn <= 0) {
            throw new DomainException(ErrorCode.INVALID_QUANTITY, "La cantidad inicial del lote debe ser mayor a 0");
        }
        if (quantityRemaining < 0) {
            throw new DomainException(ErrorCode.INVALID_QUANTITY, "La cantidad restante no puede ser negativa");
        }
        if (unitCost < 0) {
            throw new DomainException(ErrorCode.INVALID_UNIT_COST, "El costo unitario de compra no puede ser negativo");
        }
        this.id = id;
        this.productId = productId;
        this.receivedAt = receivedAt != null ? receivedAt : Instant.now();
        this.quantityIn = quantityIn;
        this.quantityRemaining = quantityRemaining;
        this.unitCost = unitCost;
        this.note = note != null ? note.trim() : "";
    }

    public StockLot(long productId, int quantityIn, long unitCost, Instant receivedAt, String note) {
        this(null, productId, receivedAt, quantityIn, quantityIn, unitCost, note);
    }

    public Long getId() {
        return id;
    }

    public long getProductId() {
        return productId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public int getQuantityIn() {
        return quantityIn;
    }

    public int getQuantityRemaining() {
        return quantityRemaining;
    }

    public long getUnitCost() {
        return unitCost;
    }

    public String getNote() {
        return note;
    }

    public StockLot withId(Long newId) {
        return new StockLot(newId, productId, receivedAt, quantityIn, quantityRemaining, unitCost, note);
    }

    public StockLot deduct(int quantityToDeduct) {
        if (quantityToDeduct <= 0) {
            throw new DomainException(ErrorCode.INVALID_QUANTITY, "La cantidad a descontar debe ser mayor a 0");
        }
        if (quantityToDeduct > quantityRemaining) {
            throw new DomainException(ErrorCode.INSUFFICIENT_STOCK, "Stock insuficiente en el lote " + id);
        }
        return new StockLot(id, productId, receivedAt, quantityIn, quantityRemaining - quantityToDeduct, unitCost, note);
    }

    public StockLot restore(int quantityToRestore) {
        if (quantityToRestore <= 0) {
            throw new DomainException(ErrorCode.INVALID_QUANTITY, "La cantidad a restaurar debe ser mayor a 0");
        }
        return new StockLot(id, productId, receivedAt, quantityIn, quantityRemaining + quantityToRestore, unitCost, note);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockLot stockLot = (StockLot) o;
        return productId == stockLot.productId &&
                quantityIn == stockLot.quantityIn &&
                quantityRemaining == stockLot.quantityRemaining &&
                unitCost == stockLot.unitCost &&
                Objects.equals(id, stockLot.id) &&
                Objects.equals(receivedAt, stockLot.receivedAt) &&
                Objects.equals(note, stockLot.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, productId, receivedAt, quantityIn, quantityRemaining, unitCost, note);
    }

    @Override
    public String toString() {
        return "StockLot{id=" + id + ", productId=" + productId + ", receivedAt=" + receivedAt +
                ", quantityIn=" + quantityIn + ", quantityRemaining=" + quantityRemaining +
                ", unitCost=" + unitCost + ", note='" + note + "'}";
    }
}
