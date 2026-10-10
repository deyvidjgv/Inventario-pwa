package com.deyvidjgv.inventario.domain.dto;

import com.deyvidjgv.inventario.domain.model.StockAdjustment;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class JornadaSummary {
    private final long jornadaId;
    private final Instant openedAt;
    private final Instant closedAt;
    private final boolean isOpen;
    private final long totalSales;
    private final long totalCost;
    private final long grossProfit;
    private final long totalExpenses;
    private final long netInformativeProfit;
    private final int salesCount;
    private final List<StockAdjustment> adjustments;

    public JornadaSummary(long jornadaId, Instant openedAt, Instant closedAt, boolean isOpen,
                          long totalSales, long totalCost, long totalExpenses,
                          int salesCount, List<StockAdjustment> adjustments) {
        this.jornadaId = jornadaId;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.isOpen = isOpen;
        this.totalSales = totalSales;
        this.totalCost = totalCost;
        this.grossProfit = totalSales - totalCost;
        this.totalExpenses = totalExpenses;
        this.netInformativeProfit = this.grossProfit - totalExpenses;
        this.salesCount = salesCount;
        this.adjustments = adjustments != null ? Collections.unmodifiableList(adjustments) : Collections.emptyList();
    }

    public long getJornadaId() {
        return jornadaId;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public long getTotalSales() {
        return totalSales;
    }

    public long getTotalCost() {
        return totalCost;
    }

    public long getGrossProfit() {
        return grossProfit;
    }

    public long getTotalExpenses() {
        return totalExpenses;
    }

    public long getNetInformativeProfit() {
        return netInformativeProfit;
    }

    public int getSalesCount() {
        return salesCount;
    }

    public List<StockAdjustment> getAdjustments() {
        return adjustments;
    }
}
