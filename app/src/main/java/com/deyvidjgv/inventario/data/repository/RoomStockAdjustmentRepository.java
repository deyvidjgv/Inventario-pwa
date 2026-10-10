package com.deyvidjgv.inventario.data.repository;

import com.deyvidjgv.inventario.data.local.dao.StockAdjustmentDao;
import com.deyvidjgv.inventario.data.local.entity.StockAdjustmentEntity;
import com.deyvidjgv.inventario.domain.model.StockAdjustment;
import com.deyvidjgv.inventario.domain.port.StockAdjustmentRepository;

import java.util.ArrayList;
import java.util.List;

public class RoomStockAdjustmentRepository implements StockAdjustmentRepository {
    private final StockAdjustmentDao stockAdjustmentDao;

    public RoomStockAdjustmentRepository(StockAdjustmentDao stockAdjustmentDao) {
        this.stockAdjustmentDao = stockAdjustmentDao;
    }

    @Override
    public StockAdjustment save(StockAdjustment adjustment) {
        StockAdjustmentEntity entity = StockAdjustmentEntity.fromDomain(adjustment);
        long id = stockAdjustmentDao.insert(entity);
        return adjustment.withId(id);
    }

    @Override
    public List<StockAdjustment> findByJornadaId(long jornadaId) {
        List<StockAdjustmentEntity> entities = stockAdjustmentDao.findByJornadaId(jornadaId);
        List<StockAdjustment> list = new ArrayList<>();
        for (StockAdjustmentEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<StockAdjustment> findAll() {
        List<StockAdjustmentEntity> entities = stockAdjustmentDao.findAll();
        List<StockAdjustment> list = new ArrayList<>();
        for (StockAdjustmentEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public void deleteAll() {
        stockAdjustmentDao.deleteAll();
    }
}
