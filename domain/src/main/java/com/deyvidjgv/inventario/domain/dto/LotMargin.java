package com.deyvidjgv.inventario.domain.dto;

import java.util.Objects;

public class LotMargin {
    private final long lotId;
    private final long unitCost;
    private final int quantityRemaining;
    private final long profitPerUnit;
    private final double marginOnSale;
    private final Double marginOnCost; // null si el costo es 0

    public LotMargin(long lotId, long unitCost, int quantityRemaining, long salePrice) {
        this.lotId = lotId;
        this.unitCost = unitCost;
        this.quantityRemaining = quantityRemaining;
        this.profitPerUnit = salePrice - unitCost;

        if (salePrice > 0) {
            this.marginOnSale = Math.round(((double) this.profitPerUnit / salePrice) * 1000.0) / 10.0;
        } else {
            this.marginOnSale = 0.0;
        }

        if (unitCost > 0) {
            this.marginOnCost = Math.round(((double) this.profitPerUnit / unitCost) * 1000.0) / 10.0;
        } else {
            this.marginOnCost = null;
        }
    }

    public long getLotId() {
        return lotId;
    }

    public long getUnitCost() {
        return unitCost;
    }

    public int getQuantityRemaining() {
        return quantityRemaining;
    }

    public long getProfitPerUnit() {
        return profitPerUnit;
    }

    public double getMarginOnSale() {
        return marginOnSale;
    }

    public Double getMarginOnCost() {
        return marginOnCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LotMargin lotMargin = (LotMargin) o;
        return lotId == lotMargin.lotId &&
                unitCost == lotMargin.unitCost &&
                quantityRemaining == lotMargin.quantityRemaining &&
                profitPerUnit == lotMargin.profitPerUnit &&
                Double.compare(marginOnSale, lotMargin.marginOnSale) == 0 &&
                Objects.equals(marginOnCost, lotMargin.marginOnCost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lotId, unitCost, quantityRemaining, profitPerUnit, marginOnSale, marginOnCost);
    }

    @Override
    public String toString() {
        return "LotMargin{lotId=" + lotId + ", unitCost=" + unitCost +
                ", quantityRemaining=" + quantityRemaining + ", profitPerUnit=" + profitPerUnit +
                ", marginOnSale=" + marginOnSale + "%, marginOnCost=" + marginOnCost + "%}";
    }
}
