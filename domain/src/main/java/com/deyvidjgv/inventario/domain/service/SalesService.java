package com.deyvidjgv.inventario.domain.service;

import com.deyvidjgv.inventario.domain.dto.JornadaSummary;
import com.deyvidjgv.inventario.domain.model.Jornada;
import com.deyvidjgv.inventario.domain.model.Sale;

import java.time.Instant;
import java.util.Map;

public interface SalesService {
    Jornada openJornada(Instant at);
    JornadaSummary closeJornada(long jornadaId, Instant at, Map<Long, Integer> countedByProduct);
    Jornada reopenLastJornada();
    Sale sell(long productId, int quantity);
    void voidSale(long saleId);
}
