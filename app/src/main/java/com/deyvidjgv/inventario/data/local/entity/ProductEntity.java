package com.deyvidjgv.inventario.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.deyvidjgv.inventario.domain.model.Product;

@Entity(
    tableName = "products",
    foreignKeys = @ForeignKey(
        entity = CategoryEntity.class,
        parentColumns = "id",
        childColumns = "categoryId",
        onDelete = ForeignKey.RESTRICT
    ),
    indices = {@Index("categoryId")}
)
public class ProductEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    public String name;
    public long categoryId;
    public long salePrice;
    public boolean tracksStock;
    public boolean active;

    public ProductEntity() {}

    @Ignore
    public ProductEntity(Long id, String name, long categoryId, long salePrice, boolean tracksStock, boolean active) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.salePrice = salePrice;
        this.tracksStock = tracksStock;
        this.active = active;
    }

    public static ProductEntity fromDomain(Product product) {
        return new ProductEntity(
            product.getId(),
            product.getName(),
            product.getCategoryId(),
            product.getSalePrice(),
            product.isTracksStock(),
            product.isActive()
        );
    }

    public Product toDomain() {
        return new Product(id, name, categoryId, salePrice, tracksStock, active);
    }
}
