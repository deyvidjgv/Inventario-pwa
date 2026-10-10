package com.deyvidjgv.inventario.domain.dto;

import com.deyvidjgv.inventario.domain.model.*;

import java.util.ArrayList;
import java.util.List;

public class BackupPayload {
    private int version = 1;
    private String exportedAt;
    private List<Category> categories = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private List<StockLot> lots = new ArrayList<>();
    private List<Jornada> jornadas = new ArrayList<>();
    private List<Sale> sales = new ArrayList<>();
    private List<SaleLotAllocation> allocations = new ArrayList<>();
    private List<StockAdjustment> adjustments = new ArrayList<>();
    private List<Expense> expenses = new ArrayList<>();
    private List<AuditLog> auditLogs = new ArrayList<>();

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(String exportedAt) {
        this.exportedAt = exportedAt;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public List<StockLot> getLots() {
        return lots;
    }

    public void setLots(List<StockLot> lots) {
        this.lots = lots;
    }

    public List<Jornada> getJornadas() {
        return jornadas;
    }

    public void setJornadas(List<Jornada> jornadas) {
        this.jornadas = jornadas;
    }

    public List<Sale> getSales() {
        return sales;
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    public List<SaleLotAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<SaleLotAllocation> allocations) {
        this.allocations = allocations;
    }

    public List<StockAdjustment> getAdjustments() {
        return adjustments;
    }

    public void setAdjustments(List<StockAdjustment> adjustments) {
        this.adjustments = adjustments;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }
}
