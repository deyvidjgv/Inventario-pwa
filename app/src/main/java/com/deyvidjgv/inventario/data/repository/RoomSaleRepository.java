package com.deyvidjgv.inventario.data.repository;

import com.deyvidjgv.inventario.data.local.dao.SaleDao;
import com.deyvidjgv.inventario.data.local.entity.SaleEntity;
import com.deyvidjgv.inventario.data.local.entity.SaleLotAllocationEntity;
import com.deyvidjgv.inventario.domain.model.Sale;
import com.deyvidjgv.inventario.domain.model.SaleLotAllocation;
import com.deyvidjgv.inventario.domain.port.SaleRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomSaleRepository implements SaleRepository {
    private final SaleDao saleDao;

    public RoomSaleRepository(SaleDao saleDao) {
        this.saleDao = saleDao;
    }

    @Override
    public Sale save(Sale sale, List<SaleLotAllocation> allocations) {
        SaleEntity saleEntity = SaleEntity.fromDomain(sale);
        List<SaleLotAllocationEntity> allocEntities = new ArrayList<>();
        if (allocations != null) {
            for (SaleLotAllocation alloc : allocations) {
                allocEntities.add(SaleLotAllocationEntity.fromDomain(alloc));
            }
        }
        long id = saleDao.insertSaleWithAllocations(saleEntity, allocEntities);
        return sale.withId(id);
    }

    @Override
    public void update(Sale sale) {
        saleDao.updateSale(SaleEntity.fromDomain(sale));
    }

    @Override
    public Optional<Sale> findById(long id) {
        SaleEntity entity = saleDao.findById(id);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<Sale> findByJornadaId(long jornadaId) {
        List<SaleEntity> entities = saleDao.findByJornadaId(jornadaId);
        List<Sale> list = new ArrayList<>();
        for (SaleEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<Sale> findByPeriod(Instant from, Instant to) {
        List<SaleEntity> entities = saleDao.findByPeriod(from, to);
        List<Sale> list = new ArrayList<>();
        for (SaleEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<Sale> findByProductId(long productId) {
        List<SaleEntity> entities = saleDao.findByProductId(productId);
        List<Sale> list = new ArrayList<>();
        for (SaleEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<SaleLotAllocation> findAllocationsBySaleId(long saleId) {
        List<SaleLotAllocationEntity> entities = saleDao.findAllocationsBySaleId(saleId);
        List<SaleLotAllocation> list = new ArrayList<>();
        for (SaleLotAllocationEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<SaleLotAllocation> findAllocationsBySaleIds(List<Long> saleIds) {
        if (saleIds == null || saleIds.isEmpty()) return java.util.Collections.emptyList();
        List<SaleLotAllocationEntity> entities = saleDao.findAllocationsBySaleIds(saleIds);
        List<SaleLotAllocation> list = new ArrayList<>();
        for (SaleLotAllocationEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<SaleLotAllocation> findAllocationsByProductId(long productId) {
        List<SaleLotAllocationEntity> entities = saleDao.findAllocationsByProductId(productId);
        List<SaleLotAllocation> list = new ArrayList<>();
        for (SaleLotAllocationEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<Sale> findAll() {
        List<SaleEntity> entities = saleDao.findAll();
        List<Sale> list = new ArrayList<>();
        for (SaleEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<SaleLotAllocation> findAllAllocations() {
        List<SaleLotAllocationEntity> entities = saleDao.findAllAllocations();
        List<SaleLotAllocation> list = new ArrayList<>();
        for (SaleLotAllocationEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public void deleteAll() {
        saleDao.deleteAll();
    }
}
