package com.deyvidjgv.inventario.domain.dto;

import com.deyvidjgv.inventario.domain.model.Expense;
import com.deyvidjgv.inventario.domain.model.StockAdjustment;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class DailyReport {
    private final LocalDate date;
    private final long totalSales;
    private final long totalCost;
    private final long grossProfit;
    private final long totalExpenses;
    private final long netProfit;
    private final int salesCount;
    private final List<ProductSaleDetail> productsSold;
    private final List<StockEntryDetail> stockEntries;
    private final List<Expense> expenses;
    private final List<StockAdjustmentDetail> adjustments;

    public DailyReport(LocalDate date,
                       long totalSales,
                       long totalCost,
                       long totalExpenses,
                       int salesCount,
                       List<ProductSaleDetail> productsSold,
                       List<StockEntryDetail> stockEntries,
                       List<Expense> expenses,
                       List<StockAdjustmentDetail> adjustments) {
        this.date = date;
        this.totalSales = totalSales;
        this.totalCost = totalCost;
        this.grossProfit = totalSales - totalCost;
        this.totalExpenses = totalExpenses;
        this.netProfit = this.grossProfit - totalExpenses;
        this.salesCount = salesCount;
        this.productsSold = productsSold != null ? Collections.unmodifiableList(productsSold) : Collections.emptyList();
        this.stockEntries = stockEntries != null ? Collections.unmodifiableList(stockEntries) : Collections.emptyList();
        this.expenses = expenses != null ? Collections.unmodifiableList(expenses) : Collections.emptyList();
        this.adjustments = adjustments != null ? Collections.unmodifiableList(adjustments) : Collections.emptyList();
    }

    public LocalDate getDate() {
        return date;
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

    public long getNetProfit() {
        return netProfit;
    }

    public int getSalesCount() {
        return salesCount;
    }

    public double getNetMarginPercent() {
        if (totalSales > 0) {
            return Math.round(((double) netProfit / totalSales) * 1000.0) / 10.0;
        }
        return 0.0;
    }

    public double getGrossMarginPercent() {
        if (totalSales > 0) {
            return Math.round(((double) grossProfit / totalSales) * 1000.0) / 10.0;
        }
        return 0.0;
    }

    public List<ProductSaleDetail> getProductsSold() {
        return productsSold;
    }

    public List<StockEntryDetail> getStockEntries() {
        return stockEntries;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public List<StockAdjustmentDetail> getAdjustments() {
        return adjustments;
    }

    public long getTotalShrinkageLoss() {
        long loss = 0;
        for (StockAdjustmentDetail adj : adjustments) {
            if (adj.getDifference() < 0) {
                loss += Math.abs(adj.getTotalLossOrGainAtCost());
            }
        }
        return loss;
    }
}
