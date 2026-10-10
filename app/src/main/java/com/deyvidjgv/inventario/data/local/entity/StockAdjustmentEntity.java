package com.deyvidjgv.inventario.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.deyvidjgv.inventario.domain.model.StockAdjustment;

import java.time.Instant;

@Entity(
    tableName = "stock_adjustments",
    foreignKeys = {
        @ForeignKey(
            entity = ProductEntity.class,
            parentColumns = "id",
            childColumns = "productId",
            onDelete = ForeignKey.RESTRICT
        ),
        @ForeignKey(
            entity = JornadaEntity.class,
            parentColumns = "id",
            childColumns = "jornadaId",
            onDelete = ForeignKey.RESTRICT
        )
    },
    indices = {@Index("productId"), @Index("jornadaId")}
)
public class StockAdjustmentEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    public long productId;
    public long jornadaId;
    public int expected;
    public int counted;
    public int difference;
    public Instant createdAt;

    public StockAdjustmentEntity() {}

    @Ignore
    public StockAdjustmentEntity(Long id, long productId, long jornadaId, int expected, int counted, int difference, Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.jornadaId = jornadaId;
        this.expected = expected;
        this.counted = counted;
        this.difference = difference;
        this.createdAt = createdAt;
    }

    public static StockAdjustmentEntity fromDomain(StockAdjustment adj) {
        return new StockAdjustmentEntity(
            adj.getId(),
            adj.getProductId(),
            adj.getJornadaId(),
            adj.getExpected(),
            adj.getCounted(),
            adj.getDifference(),
            adj.getCreatedAt()
        );
    }

    public StockAdjustment toDomain() {
        return new StockAdjustment(id, productId, jornadaId, expected, counted, difference, createdAt);
    }
}
