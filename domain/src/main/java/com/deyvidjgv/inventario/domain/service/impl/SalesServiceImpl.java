package com.deyvidjgv.inventario.domain.service.impl;

import com.deyvidjgv.inventario.domain.dto.JornadaSummary;
import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;
import com.deyvidjgv.inventario.domain.model.*;
import com.deyvidjgv.inventario.domain.port.*;
import com.deyvidjgv.inventario.domain.service.SalesService;

import java.time.Instant;
import java.util.*;

public class SalesServiceImpl implements SalesService {
    private final JornadaRepository jornadaRepository;
    private final ProductRepository productRepository;
    private final StockLotRepository stockLotRepository;
    private final SaleRepository saleRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ExpenseRepository expenseRepository;
    private final AuditLogRepository auditLogRepository;
    private final TransactionManager transactionManager;

    public SalesServiceImpl(JornadaRepository jornadaRepository,
                            ProductRepository productRepository,
                            StockLotRepository stockLotRepository,
                            SaleRepository saleRepository,
                            StockAdjustmentRepository stockAdjustmentRepository,
                            ExpenseRepository expenseRepository,
                            AuditLogRepository auditLogRepository,
                            TransactionManager transactionManager) {
        this.jornadaRepository = jornadaRepository;
        this.productRepository = productRepository;
        this.stockLotRepository = stockLotRepository;
        this.saleRepository = saleRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.expenseRepository = expenseRepository;
        this.auditLogRepository = auditLogRepository;
        this.transactionManager = transactionManager;
    }

    @Override
    public Optional<Jornada> getOpenJornada() {
        return jornadaRepository.findOpen();
    }

    @Override
    public Optional<Jornada> getLastClosedJornada() {
        return jornadaRepository.findLastClosed();
    }

    @Override
    public List<Sale> getSalesForJornada(long jornadaId) {
        return saleRepository.findByJornadaId(jornadaId);
    }

    @Override
    public Optional<Sale> getLastNonVoidedSale(long jornadaId) {
        List<Sale> sales = saleRepository.findByJornadaId(jornadaId);
        return sales.stream()
                .filter(s -> !s.isVoided())
                .max(Comparator.comparing(Sale::getCreatedAt));
    }

    @Override
    public Jornada openJornada(Instant at) {
        return transactionManager.executeInTransaction(() -> {
            Optional<Jornada> existingOpen = jornadaRepository.findOpen();
            if (existingOpen.isPresent()) {
                throw new DomainException(ErrorCode.JORNADA_ALREADY_OPEN, "Ya existe una jornada abierta actualmente: " + existingOpen.get().getId());
            }
            Instant openedAt = at != null ? at : Instant.now();
            return jornadaRepository.save(new Jornada(openedAt));
        });
    }

    @Override
    public Jornada reopenLastJornada() {
        return transactionManager.executeInTransaction(() -> {
            Optional<Jornada> currentOpen = jornadaRepository.findOpen();
            if (currentOpen.isPresent()) {
                throw new DomainException(ErrorCode.JORNADA_ALREADY_OPEN, "No se puede reabrir una jornada porque ya hay una abierta");
            }
            Jornada lastClosed = jornadaRepository.findLastClosed()
                    .orElseThrow(() -> new DomainException(ErrorCode.NO_CLOSED_JORNADA_TO_REOPEN, "No hay ninguna jornada cerrada para reabrir"));

            Jornada reopened = lastClosed.reopen();
            return jornadaRepository.save(reopened);
        });
    }

    @Override
    public Sale sell(long productId, int quantity) {
        return sell(productId, quantity, null);
    }

    @Override
    public Sale sell(long productId, int quantity, Instant at) {
        return transactionManager.executeInTransaction(() -> {
            Jornada openJornada = jornadaRepository.findOpen()
                    .orElseThrow(() -> new DomainException(ErrorCode.NO_OPEN_JORNADA, "No hay una jornada abierta. Debe abrir jornada para registrar ventas."));

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "Producto no encontrado: " + productId));

            if (!product.isActive()) {
                throw new DomainException(ErrorCode.PRODUCT_INACTIVE, "No se puede vender un producto archivado");
            }
            if (quantity <= 0) {
                throw new DomainException(ErrorCode.INVALID_QUANTITY, "La cantidad a vender debe ser mayor a 0");
            }

            Instant saleTime = at != null ? at : Instant.now();
            Sale newSale = new Sale(openJornada.getId(), productId, quantity, product.getSalePrice(), saleTime);
            List<SaleLotAllocation> allocations = new ArrayList<>();

            if (product.isTracksStock()) {
                List<StockLot> activeLots = stockLotRepository.findActiveByProductId(productId);
                int totalAvailable = activeLots.stream().mapToInt(StockLot::getQuantityRemaining).sum();

                if (totalAvailable < quantity) {
                    throw new DomainException(ErrorCode.INSUFFICIENT_STOCK,
                            "Stock insuficiente para " + product.getName() + ". Solicitado: " + quantity + ", disponible: " + totalAvailable);
                }

                int remainingToAllocate = quantity;
                List<StockLot> updatedLots = new ArrayList<>();

                for (StockLot lot : activeLots) {
                    if (remainingToAllocate <= 0) break;
                    int take = Math.min(remainingToAllocate, lot.getQuantityRemaining());
                    allocations.add(new SaleLotAllocation(0L, lot.getId(), take, lot.getUnitCost()));
                    updatedLots.add(lot.deduct(take));
                    remainingToAllocate -= take;
                }

                // Guardar los lotes actualizados
                stockLotRepository.saveAll(updatedLots);
            }

            // Guardar la venta y sus asignaciones
            Sale savedSale = saleRepository.save(newSale, allocations);
            return savedSale;
        });
    }

    @Override
    public void voidSale(long saleId) {
        transactionManager.executeInTransaction(() -> {
            Sale sale = saleRepository.findById(saleId)
                    .orElseThrow(() -> new DomainException(ErrorCode.SALE_NOT_FOUND, "Venta no encontrada: " + saleId));

            if (sale.isVoided()) {
                throw new DomainException(ErrorCode.SALE_ALREADY_VOIDED, "La venta ya se encuentra anulada");
            }

            // Restaurar lotes si tenía asignaciones
            List<SaleLotAllocation> allocations = saleRepository.findAllocationsBySaleId(saleId);
            List<StockLot> lotsToRestore = new ArrayList<>();
            for (SaleLotAllocation allocation : allocations) {
                StockLot lot = stockLotRepository.findById(allocation.getLotId())
                        .orElseThrow(() -> new DomainException(ErrorCode.LOT_NOT_FOUND, "Lote no encontrado para reversión: " + allocation.getLotId()));
                lotsToRestore.add(lot.restore(allocation.getQuantity()));
            }
            if (!lotsToRestore.isEmpty()) {
                stockLotRepository.saveAll(lotsToRestore);
            }

            // Marcar venta como anulada
            Sale voidedSale = sale.markVoided();
            saleRepository.update(voidedSale);

            // Registrar en auditoría
            AuditLog auditLog = new AuditLog(
                    "Sale",
                    saleId,
                    "VOID",
                    "{\"voided\":false,\"total\":" + sale.getTotal() + "}",
                    "{\"voided\":true}",
                    Instant.now()
            );
            auditLogRepository.save(auditLog);
        });
    }

    @Override
    public JornadaSummary closeJornada(long jornadaId, Instant at, Map<Long, Integer> countedByProduct) {
        return transactionManager.executeInTransaction(() -> {
            Jornada jornada = jornadaRepository.findById(jornadaId)
                    .orElseThrow(() -> new DomainException(ErrorCode.JORNADA_NOT_FOUND, "Jornada no encontrada: " + jornadaId));

            if (!jornada.isOpen()) {
                throw new DomainException(ErrorCode.JORNADA_NOT_FOUND, "La jornada ya se encuentra cerrada");
            }

            Instant closedAt = at != null ? at : Instant.now();
            List<StockAdjustment> adjustments = new ArrayList<>();

            // Procesar conteo físico si fue suministrado
            if (countedByProduct != null && !countedByProduct.isEmpty()) {
                for (Map.Entry<Long, Integer> entry : countedByProduct.entrySet()) {
                    long productId = entry.getKey();
                    int counted = entry.getValue();
                    if (counted < 0) {
                        throw new DomainException(ErrorCode.INVALID_QUANTITY, "El conteo físico no puede ser negativo: " + counted);
                    }

                    Product product = productRepository.findById(productId).orElse(null);
                    if (product == null || !product.isTracksStock()) {
                        continue;
                    }

                    List<StockLot> activeLots = stockLotRepository.findActiveByProductId(productId);
                    int expected = activeLots.stream().mapToInt(StockLot::getQuantityRemaining).sum();
                    int difference = counted - expected;

                    if (difference != 0) {
                        StockAdjustment adj = new StockAdjustment(productId, jornadaId, expected, counted, closedAt);
                        adjustments.add(stockAdjustmentRepository.save(adj));

                        if (difference < 0) {
                            // Faltante: descontar de lotes en orden FIFO
                            int toDeduct = Math.abs(difference);
                            List<StockLot> updatedLots = new ArrayList<>();
                            for (StockLot lot : activeLots) {
                                if (toDeduct <= 0) break;
                                int take = Math.min(toDeduct, lot.getQuantityRemaining());
                                updatedLots.add(lot.deduct(take));
                                toDeduct -= take;
                            }
                            stockLotRepository.saveAll(updatedLots);
                        } else {
                            // Sobrante: crear un nuevo lote con el costo del lote más reciente
                            Optional<StockLot> newestLot = stockLotRepository.findNewestByProductId(productId);
                            long unitCost = newestLot.map(StockLot::getUnitCost).orElse(0L);
                            StockLot surplusLot = new StockLot(productId, difference, unitCost, closedAt, "Ajuste conteo físico");
                            stockLotRepository.save(surplusLot);
                        }
                    }
                }
            }

            // Cerrar la jornada
            Jornada closedJornada = jornada.close(closedAt);
            jornadaRepository.save(closedJornada);

            // Calcular resumen financiero
            return calculateSummary(jornadaId, closedJornada.getOpenedAt(), closedAt, false, adjustments);
        });
    }

    private JornadaSummary calculateSummary(long jornadaId, Instant openedAt, Instant closedAt, boolean isOpen, List<StockAdjustment> adjustments) {
        List<Sale> sales = saleRepository.findByJornadaId(jornadaId);
        long totalSales = 0;
        long totalCost = 0;
        int salesCount = 0;

        for (Sale sale : sales) {
            if (!sale.isVoided()) {
                totalSales += sale.getTotal();
                salesCount++;
                List<SaleLotAllocation> allocations = saleRepository.findAllocationsBySaleId(sale.getId());
                for (SaleLotAllocation alloc : allocations) {
                    totalCost += alloc.getTotalCost();
                }
            }
        }

        List<Expense> expenses = expenseRepository.findByJornadaId(jornadaId);
        long totalExpenses = expenses.stream().mapToLong(Expense::getAmount).sum();

        if (adjustments == null) {
            adjustments = stockAdjustmentRepository.findByJornadaId(jornadaId);
        }

        return new JornadaSummary(
                jornadaId,
                openedAt,
                closedAt,
                isOpen,
                totalSales,
                totalCost,
                totalExpenses,
                salesCount,
                adjustments
        );
    }
}
