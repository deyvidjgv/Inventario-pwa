package com.deyvidjgv.inventario.domain.port;

import com.deyvidjgv.inventario.domain.model.StockAdjustment;

import java.util.List;

public interface StockAdjustmentRepository {
    StockAdjustment save(StockAdjustment adjustment);
    List<StockAdjustment> findByJornadaId(long jornadaId);
    List<StockAdjustment> findAll();
    void deleteAll();
}
