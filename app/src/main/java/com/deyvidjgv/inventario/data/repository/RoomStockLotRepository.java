package com.deyvidjgv.inventario.data.repository;

import com.deyvidjgv.inventario.data.local.dao.StockLotDao;
import com.deyvidjgv.inventario.data.local.entity.StockLotEntity;
import com.deyvidjgv.inventario.domain.model.StockLot;
import com.deyvidjgv.inventario.domain.port.StockLotRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomStockLotRepository implements StockLotRepository {
    private final StockLotDao stockLotDao;

    public RoomStockLotRepository(StockLotDao stockLotDao) {
        this.stockLotDao = stockLotDao;
    }

    @Override
    public StockLot save(StockLot lot) {
        StockLotEntity entity = StockLotEntity.fromDomain(lot);
        long id = stockLotDao.insert(entity);
        return lot.withId(id);
    }

    @Override
    public List<StockLot> saveAll(List<StockLot> lots) {
        List<StockLotEntity> entities = new ArrayList<>();
        for (StockLot lot : lots) {
            entities.add(StockLotEntity.fromDomain(lot));
        }
        stockLotDao.updateAll(entities);
        return lots;
    }

    @Override
    public Optional<StockLot> findById(long id) {
        StockLotEntity entity = stockLotDao.findById(id);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<StockLot> findActiveByProductId(long productId) {
        List<StockLotEntity> entities = stockLotDao.findActiveByProductId(productId);
        List<StockLot> list = new ArrayList<>();
        for (StockLotEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<StockLot> findAllByProductId(long productId) {
        List<StockLotEntity> entities = stockLotDao.findAllByProductId(productId);
        List<StockLot> list = new ArrayList<>();
        for (StockLotEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public Optional<StockLot> findNewestByProductId(long productId) {
        StockLotEntity entity = stockLotDao.findNewestByProductId(productId);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<StockLot> findAll() {
        List<StockLotEntity> entities = stockLotDao.findAll();
        List<StockLot> list = new ArrayList<>();
        for (StockLotEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public void deleteAll() {
        stockLotDao.deleteAll();
    }
}
