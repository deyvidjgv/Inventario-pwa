package com.deyvidjgv.inventario.domain.model;

import java.time.Instant;
import java.util.Objects;

public class StockAdjustment {
    private final Long id;
    private final long productId;
    private final long jornadaId;
    private final int expected;
    private final int counted;
    private final int difference;
    private final Instant createdAt;

    public StockAdjustment(Long id, long productId, long jornadaId, int expected, int counted, int difference, Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.jornadaId = jornadaId;
        this.expected = expected;
        this.counted = counted;
        this.difference = difference;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public StockAdjustment(long productId, long jornadaId, int expected, int counted, Instant createdAt) {
        this(null, productId, jornadaId, expected, counted, counted - expected, createdAt);
    }

    public Long getId() {
        return id;
    }

    public long getProductId() {
        return productId;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public StockAdjustment withId(Long newId) {
        return new StockAdjustment(newId, productId, jornadaId, expected, counted, difference, createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockAdjustment that = (StockAdjustment) o;
        return productId == that.productId &&
                jornadaId == that.jornadaId &&
                expected == that.expected &&
                counted == that.counted &&
                difference == that.difference &&
                Objects.equals(id, that.id) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, productId, jornadaId, expected, counted, difference, createdAt);
    }

    @Override
    public String toString() {
        return "StockAdjustment{id=" + id + ", productId=" + productId + ", jornadaId=" + jornadaId +
                ", expected=" + expected + ", counted=" + counted + ", difference=" + difference +
                ", createdAt=" + createdAt + "}";
    }
}
