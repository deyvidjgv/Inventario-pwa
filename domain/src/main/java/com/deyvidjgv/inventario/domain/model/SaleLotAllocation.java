package com.deyvidjgv.inventario.domain.model;

import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;

import java.util.Objects;

public class SaleLotAllocation {
    private final long saleId;
    private final long lotId;
    private final int quantity;
    private final long unitCost;

    public SaleLotAllocation(long saleId, long lotId, int quantity, long unitCost) {
        if (quantity <= 0) {
            throw new DomainException(ErrorCode.INVALID_QUANTITY, "La cantidad asignada debe ser mayor a 0");
        }
        if (unitCost < 0) {
            throw new DomainException(ErrorCode.INVALID_UNIT_COST, "El costo unitario de la asignación no puede ser negativo");
        }
        this.saleId = saleId;
        this.lotId = lotId;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public long getSaleId() {
        return saleId;
    }

    public long getLotId() {
        return lotId;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitCost() {
        return unitCost;
    }

    public long getTotalCost() {
        return unitCost * quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaleLotAllocation that = (SaleLotAllocation) o;
        return saleId == that.saleId &&
                lotId == that.lotId &&
                quantity == that.quantity &&
                unitCost == that.unitCost;
    }

    @Override
    public int hashCode() {
        return Objects.hash(saleId, lotId, quantity, unitCost);
    }

    @Override
    public String toString() {
        return "SaleLotAllocation{saleId=" + saleId + ", lotId=" + lotId +
                ", quantity=" + quantity + ", unitCost=" + unitCost + "}";
    }
}
