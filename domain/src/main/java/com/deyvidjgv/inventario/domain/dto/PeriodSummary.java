package com.deyvidjgv.inventario.domain.dto;

import java.time.LocalDate;

public class PeriodSummary {
    private final LocalDate from;
    private final LocalDate to;
    private final long totalSales;
    private final long totalCost;
    private final long grossProfit;
    private final long totalExpenses;
    private final long netInformativeProfit;
    private final int salesCount;

    public PeriodSummary(LocalDate from, LocalDate to, long totalSales, long totalCost, long totalExpenses, int salesCount) {
        this.from = from;
        this.to = to;
        this.totalSales = totalSales;
        this.totalCost = totalCost;
        this.grossProfit = totalSales - totalCost;
        this.totalExpenses = totalExpenses;
        this.netInformativeProfit = this.grossProfit - totalExpenses;
        this.salesCount = salesCount;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
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
}
