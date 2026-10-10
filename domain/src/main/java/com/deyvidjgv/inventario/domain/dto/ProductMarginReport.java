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

    public ProductMarginReport(long productId, String productName, long currentSalePrice,
                               Long newestLotCost, List<LotMargin> lotMargins,
                               long realProfit, long profitWithNewCost) {
        this.productId = productId;
        this.productName = productName != null ? productName : "";
        this.currentSalePrice = currentSalePrice;
        this.newestLotCost = newestLotCost;
        this.lotMargins = lotMargins != null ? Collections.unmodifiableList(lotMargins) : Collections.emptyList();
        this.realProfit = realProfit;
        this.profitWithNewCost = profitWithNewCost;
        this.difference = profitWithNewCost - realProfit;
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
}
