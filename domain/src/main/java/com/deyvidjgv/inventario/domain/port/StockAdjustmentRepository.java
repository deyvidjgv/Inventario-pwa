package com.deyvidjgv.inventario.domain.port;

import com.deyvidjgv.inventario.domain.model.StockAdjustment;

import java.time.Instant;
import java.util.List;

public interface StockAdjustmentRepository {
    StockAdjustment save(StockAdjustment adjustment);
    List<StockAdjustment> findByJornadaId(long jornadaId);
    List<StockAdjustment> findByPeriod(Instant from, Instant to);
    List<StockAdjustment> findAll();
    void deleteAll();
}
