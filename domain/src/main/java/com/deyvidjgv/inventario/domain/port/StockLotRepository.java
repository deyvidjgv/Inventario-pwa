package com.deyvidjgv.inventario.domain.port;

import com.deyvidjgv.inventario.domain.model.StockLot;

import java.util.List;
import java.util.Optional;

public interface StockLotRepository {
    StockLot save(StockLot lot);
    List<StockLot> saveAll(List<StockLot> lots);
    Optional<StockLot> findById(long id);
    List<StockLot> findActiveByProductId(long productId); // FIFO ordenado: receivedAt ASC, id ASC
    List<StockLot> findAllByProductId(long productId);
    Optional<StockLot> findNewestByProductId(long productId); // receivedAt DESC, id DESC
    List<StockLot> findAll();
    void deleteAll();
}
