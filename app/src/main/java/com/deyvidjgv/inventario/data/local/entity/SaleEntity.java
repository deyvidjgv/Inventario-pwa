package com.deyvidjgv.inventario.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.deyvidjgv.inventario.domain.model.Sale;

import java.time.Instant;

@Entity(
    tableName = "sales",
    foreignKeys = {
        @ForeignKey(
            entity = JornadaEntity.class,
            parentColumns = "id",
            childColumns = "jornadaId",
            onDelete = ForeignKey.RESTRICT
        ),
        @ForeignKey(
            entity = ProductEntity.class,
            parentColumns = "id",
            childColumns = "productId",
            onDelete = ForeignKey.RESTRICT
        )
    },
    indices = {@Index("jornadaId"), @Index("productId"), @Index("createdAt")}
)
public class SaleEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    public long jornadaId;
    public long productId;
    public int quantity;
    public long unitPrice;
    public Instant createdAt;
    public boolean voided;

    public SaleEntity() {}

    public SaleEntity(Long id, long jornadaId, long productId, int quantity, long unitPrice, Instant createdAt, boolean voided) {
        this.id = id;
        this.jornadaId = jornadaId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.createdAt = createdAt;
        this.voided = voided;
    }

    public static SaleEntity fromDomain(Sale sale) {
        return new SaleEntity(
            sale.getId(),
            sale.getJornadaId(),
            sale.getProductId(),
            sale.getQuantity(),
            sale.getUnitPrice(),
            sale.getCreatedAt(),
            sale.isVoided()
        );
    }

    public Sale toDomain() {
        return new Sale(id, jornadaId, productId, quantity, unitPrice, createdAt, voided);
    }
}
