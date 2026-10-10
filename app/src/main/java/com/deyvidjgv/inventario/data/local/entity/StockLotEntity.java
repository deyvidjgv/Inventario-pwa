package com.deyvidjgv.inventario.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.deyvidjgv.inventario.domain.model.StockLot;

import java.time.Instant;

@Entity(
    tableName = "stock_lots",
    foreignKeys = @ForeignKey(
        entity = ProductEntity.class,
        parentColumns = "id",
        childColumns = "productId",
        onDelete = ForeignKey.RESTRICT
    ),
    indices = {@Index("productId"), @Index("receivedAt")}
)
public class StockLotEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    public long productId;
    public Instant receivedAt;
    public int quantityIn;
    public int quantityRemaining;
    public long unitCost;
    public String note;

    public StockLotEntity() {}

    public StockLotEntity(Long id, long productId, Instant receivedAt, int quantityIn, int quantityRemaining, long unitCost, String note) {
        this.id = id;
        this.productId = productId;
        this.receivedAt = receivedAt;
        this.quantityIn = quantityIn;
        this.quantityRemaining = quantityRemaining;
        this.unitCost = unitCost;
        this.note = note;
    }

    public static StockLotEntity fromDomain(StockLot lot) {
        return new StockLotEntity(
            lot.getId(),
            lot.getProductId(),
            lot.getReceivedAt(),
            lot.getQuantityIn(),
            lot.getQuantityRemaining(),
            lot.getUnitCost(),
            lot.getNote()
        );
    }

    public StockLot toDomain() {
        return new StockLot(id, productId, receivedAt, quantityIn, quantityRemaining, unitCost, note);
    }
}
