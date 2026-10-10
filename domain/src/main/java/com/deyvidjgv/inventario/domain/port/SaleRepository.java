package com.deyvidjgv.inventario.domain.port;

import com.deyvidjgv.inventario.domain.model.Sale;
import com.deyvidjgv.inventario.domain.model.SaleLotAllocation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SaleRepository {
    Sale save(Sale sale, List<SaleLotAllocation> allocations);
    void update(Sale sale);
    Optional<Sale> findById(long id);
    List<Sale> findByJornadaId(long jornadaId);
    List<Sale> findByPeriod(Instant from, Instant to);
    List<Sale> findByProductId(long productId);
    List<SaleLotAllocation> findAllocationsBySaleId(long saleId);
    List<SaleLotAllocation> findAllocationsBySaleIds(List<Long> saleIds);
    List<SaleLotAllocation> findAllocationsByProductId(long productId);
    List<Sale> findAll();
    List<SaleLotAllocation> findAllAllocations();
    void deleteAll();
}
