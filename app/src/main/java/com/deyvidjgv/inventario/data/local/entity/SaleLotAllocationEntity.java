package com.deyvidjgv.inventario.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import com.deyvidjgv.inventario.domain.model.SaleLotAllocation;

@Entity(
    tableName = "sale_lot_allocations",
    primaryKeys = {"saleId", "lotId"},
    foreignKeys = {
        @ForeignKey(
            entity = SaleEntity.class,
            parentColumns = "id",
            childColumns = "saleId",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = StockLotEntity.class,
            parentColumns = "id",
            childColumns = "lotId",
            onDelete = ForeignKey.RESTRICT
        )
    },
    indices = {@Index("saleId"), @Index("lotId")}
)
public class SaleLotAllocationEntity {
    public long saleId;
    public long lotId;
    public int quantity;
    public long unitCost;

    public SaleLotAllocationEntity() {}

    @Ignore
    public SaleLotAllocationEntity(long saleId, long lotId, int quantity, long unitCost) {
        this.saleId = saleId;
        this.lotId = lotId;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public static SaleLotAllocationEntity fromDomain(SaleLotAllocation alloc) {
        return new SaleLotAllocationEntity(alloc.getSaleId(), alloc.getLotId(), alloc.getQuantity(), alloc.getUnitCost());
    }

    public SaleLotAllocation toDomain() {
        return new SaleLotAllocation(saleId, lotId, quantity, unitCost);
    }
}
