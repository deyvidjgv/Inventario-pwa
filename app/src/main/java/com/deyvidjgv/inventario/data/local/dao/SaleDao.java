package com.deyvidjgv.inventario.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.deyvidjgv.inventario.data.local.entity.SaleEntity;
import com.deyvidjgv.inventario.data.local.entity.SaleLotAllocationEntity;

import java.time.Instant;
import java.util.List;

@Dao
public abstract class SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long insertSale(SaleEntity sale);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertAllocations(List<SaleLotAllocationEntity> allocations);

    @Transaction
    public long insertSaleWithAllocations(SaleEntity sale, List<SaleLotAllocationEntity> allocations) {
        long saleId = insertSale(sale);
        if (allocations != null && !allocations.isEmpty()) {
            for (SaleLotAllocationEntity alloc : allocations) {
                alloc.saleId = saleId;
            }
            insertAllocations(allocations);
        }
        return saleId;
    }

    @Update
    public abstract void updateSale(SaleEntity sale);

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    public abstract SaleEntity findById(long id);

    @Query("SELECT * FROM sales WHERE jornadaId = :jornadaId ORDER BY createdAt ASC")
    public abstract List<SaleEntity> findByJornadaId(long jornadaId);

    @Query("SELECT * FROM sales WHERE createdAt >= :from AND createdAt <= :to ORDER BY createdAt ASC")
    public abstract List<SaleEntity> findByPeriod(Instant from, Instant to);

    @Query("SELECT * FROM sales WHERE productId = :productId ORDER BY createdAt ASC")
    public abstract List<SaleEntity> findByProductId(long productId);

    @Query("SELECT * FROM sale_lot_allocations WHERE saleId = :saleId")
    public abstract List<SaleLotAllocationEntity> findAllocationsBySaleId(long saleId);

    @Query("SELECT a.* FROM sale_lot_allocations a INNER JOIN sales s ON a.saleId = s.id WHERE s.productId = :productId")
    public abstract List<SaleLotAllocationEntity> findAllocationsByProductId(long productId);

    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    public abstract List<SaleEntity> findAll();

    @Query("SELECT * FROM sale_lot_allocations")
    public abstract List<SaleLotAllocationEntity> findAllAllocations();

    @Query("DELETE FROM sales")
    public abstract void deleteAllSales();

    @Query("DELETE FROM sale_lot_allocations")
    public abstract void deleteAllAllocations();

    @Transaction
    public void deleteAll() {
        deleteAllAllocations();
        deleteAllSales();
    }
}
