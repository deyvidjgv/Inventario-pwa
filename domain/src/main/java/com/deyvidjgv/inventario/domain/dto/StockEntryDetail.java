package com.deyvidjgv.inventario.domain.dto;

import java.time.Instant;

public class StockEntryDetail {
    private final long lotId;
    private final long productId;
    private final String productName;
    private final int quantity;
    private final long unitCost;
    private final long totalInvestment;
    private final Instant receivedAt;
    private final String note;

    public StockEntryDetail(long lotId, long productId, String productName,
                            int quantity, long unitCost, Instant receivedAt, String note) {
        this.lotId = lotId;
        this.productId = productId;
        this.productName = productName != null ? productName : "";
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.totalInvestment = (long) quantity * unitCost;
        this.receivedAt = receivedAt;
        this.note = note != null ? note : "";
    }

    public long getLotId() {
        return lotId;
    }

    public long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getUnitCost() {
        return unitCost;
    }

    public long getTotalInvestment() {
        return totalInvestment;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getNote() {
        return note;
    }
}
