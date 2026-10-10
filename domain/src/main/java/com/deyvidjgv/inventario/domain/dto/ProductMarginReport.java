package com.deyvidjgv.inventario.domain.dto;

import java.util.Collections;
import java.util.List;

public class ProductMarginReport {
    private final long productId;
    private final String productName;
    private final long currentSalePrice;
    private final Long newestLotCost;
    private final List<LotMargin> lotMargins;
    private final long realProfit;
    private final long profitWithNewCost;
    private final long difference;
    private final int unitsSold;
    private final long totalSales;
    private final long totalCost;

    public ProductMarginReport(long productId, String productName, long currentSalePrice,
                               Long newestLotCost, List<LotMargin> lotMargins,
                               long realProfit, long profitWithNewCost) {
        this(productId, productName, currentSalePrice, newestLotCost, lotMargins, realProfit, profitWithNewCost, 0, 0, 0);
    }

    public ProductMarginReport(long productId, String productName, long currentSalePrice,
                               Long newestLotCost, List<LotMargin> lotMargins,
                               long realProfit, long profitWithNewCost,
                               int unitsSold, long totalSales, long totalCost) {
        this.productId = productId;
        this.productName = productName != null ? productName : "";
        this.currentSalePrice = currentSalePrice;
        this.newestLotCost = newestLotCost;
        this.lotMargins = lotMargins != null ? Collections.unmodifiableList(lotMargins) : Collections.emptyList();
        this.realProfit = realProfit;
        this.profitWithNewCost = profitWithNewCost;
        this.difference = profitWithNewCost - realProfit;
        this.unitsSold = unitsSold;
        this.totalSales = totalSales;
        this.totalCost = totalCost;
    }

    public long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public long getCurrentSalePrice() {
        return currentSalePrice;
    }

    public Long getNewestLotCost() {
        return newestLotCost;
    }

    public List<LotMargin> getLotMargins() {
        return lotMargins;
    }

    public long getRealProfit() {
        return realProfit;
    }

    public long getProfitWithNewCost() {
        return profitWithNewCost;
    }

    public long getDifference() {
        return difference;
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
}
