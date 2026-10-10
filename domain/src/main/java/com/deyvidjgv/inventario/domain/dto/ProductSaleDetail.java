package com.deyvidjgv.inventario.domain.dto;

public class ProductSaleDetail {
    private final long productId;
    private final String productName;
    private final int unitsSold;
    private final long totalSales;
    private final long totalCost;
    private final long realProfit;
    private final double marginOnSale;

    public ProductSaleDetail(long productId, String productName, int unitsSold,
                             long totalSales, long totalCost, long realProfit) {
        this.productId = productId;
        this.productName = productName != null ? productName : "";
        this.unitsSold = unitsSold;
        this.totalSales = totalSales;
        this.totalCost = totalCost;
        this.realProfit = realProfit;
        if (totalSales > 0) {
            this.marginOnSale = Math.round(((double) realProfit / totalSales) * 1000.0) / 10.0;
        } else {
            this.marginOnSale = 0.0;
        }
    }

    public long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getUnitsSold() {
        return unitsSold;
    }

    public long getTotalSales() {
        return totalSales;
    }

    public long getTotalCost() {
        return totalCost;
    }

    public long getRealProfit() {
        return realProfit;
    }

    public double getMarginOnSale() {
        return marginOnSale;
    }
}
