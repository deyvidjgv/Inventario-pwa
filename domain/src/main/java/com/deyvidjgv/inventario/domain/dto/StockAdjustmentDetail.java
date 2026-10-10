package com.deyvidjgv.inventario.domain.dto;

import java.time.Instant;

public class StockAdjustmentDetail {
    private final Long id;
    private final long productId;
    private final String productName;
    private final long jornadaId;
    private final int expected;
    private final int counted;
    private final int difference;
    private final long unitCost;
    private final long totalLossOrGainAtCost;
    private final Instant createdAt;

    public StockAdjustmentDetail(Long id,
                                 long productId,
                                 String productName,
                                 long jornadaId,
                                 int expected,
                                 int counted,
                                 int difference,
                                 long unitCost,
                                 long totalLossOrGainAtCost,
                                 Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName != null ? productName : "Producto #" + productId;
        this.jornadaId = jornadaId;
        this.expected = expected;
        this.counted = counted;
        this.difference = difference;
        this.unitCost = unitCost;
        this.totalLossOrGainAtCost = totalLossOrGainAtCost;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public long getJornadaId() {
        return jornadaId;
    }

    public int getExpected() {
        return expected;
    }

    public int getCounted() {
        return counted;
    }

    public int getDifference() {
        return difference;
    }

    public long getUnitCost() {
        return unitCost;
    }

    public long getTotalLossOrGainAtCost() {
        return totalLossOrGainAtCost;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
