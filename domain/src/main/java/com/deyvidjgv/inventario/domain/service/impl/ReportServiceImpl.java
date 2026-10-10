package com.deyvidjgv.inventario.domain.service.impl;

import com.deyvidjgv.inventario.domain.dto.JornadaSummary;
import com.deyvidjgv.inventario.domain.dto.LotMargin;
import com.deyvidjgv.inventario.domain.dto.PeriodSummary;
import com.deyvidjgv.inventario.domain.dto.ProductMarginReport;
import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;
import com.deyvidjgv.inventario.domain.model.*;
import com.deyvidjgv.inventario.domain.port.*;
import com.deyvidjgv.inventario.domain.service.ReportService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class ReportServiceImpl implements ReportService {
    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    private final ProductRepository productRepository;
    private final StockLotRepository stockLotRepository;
    private final SaleRepository saleRepository;
    private final JornadaRepository jornadaRepository;
    private final ExpenseRepository expenseRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;

    public ReportServiceImpl(ProductRepository productRepository,
                             StockLotRepository stockLotRepository,
                             SaleRepository saleRepository,
                             JornadaRepository jornadaRepository,
                             ExpenseRepository expenseRepository,
                             StockAdjustmentRepository stockAdjustmentRepository) {
        this.productRepository = productRepository;
        this.stockLotRepository = stockLotRepository;
        this.saleRepository = saleRepository;
        this.jornadaRepository = jornadaRepository;
        this.expenseRepository = expenseRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
    }

    @Override
    public ProductMarginReport margins(long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "Producto no encontrado: " + productId));

        List<StockLot> activeLots = stockLotRepository.findActiveByProductId(productId);
        List<LotMargin> lotMargins = activeLots.stream()
                .map(lot -> new LotMargin(lot.getId(), lot.getUnitCost(), lot.getQuantityRemaining(), product.getSalePrice()))
                .collect(Collectors.toList());

        Optional<StockLot> newestLot = stockLotRepository.findNewestByProductId(productId);
        Long newestLotCost = newestLot.map(StockLot::getUnitCost).orElse(null);

        // Ventas no anuladas de este producto
        List<Sale> productSales = saleRepository.findByProductId(productId).stream()
                .filter(s -> !s.isVoided())
                .collect(Collectors.toList());

        long realProfit = 0;
        long profitWithNewCost = 0;

        if (product.isTracksStock()) {
            for (Sale sale : productSales) {
                List<SaleLotAllocation> allocations = saleRepository.findAllocationsBySaleId(sale.getId());
                for (SaleLotAllocation alloc : allocations) {
                    realProfit += (sale.getUnitPrice() - alloc.getUnitCost()) * alloc.getQuantity();
                    if (newestLotCost != null) {
                        profitWithNewCost += (sale.getUnitPrice() - newestLotCost) * alloc.getQuantity();
                    } else {
                        profitWithNewCost += (sale.getUnitPrice() - alloc.getUnitCost()) * alloc.getQuantity();
                    }
                }
            }
        } else {
            // Producto sin stock (e.g. Juego Pool): costo 0, todo es ganancia
            for (Sale sale : productSales) {
                realProfit += sale.getTotal();
                profitWithNewCost += sale.getTotal();
            }
        }

        return new ProductMarginReport(
                productId,
                product.getName(),
                product.getSalePrice(),
                newestLotCost,
                lotMargins,
                realProfit,
                profitWithNewCost
        );
    }

    @Override
    public List<ProductMarginReport> allMargins() {
        return productRepository.findActive().stream()
                .map(p -> margins(p.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public JornadaSummary summary(long jornadaId) {
        Jornada jornada = jornadaRepository.findById(jornadaId)
                .orElseThrow(() -> new DomainException(ErrorCode.JORNADA_NOT_FOUND, "Jornada no encontrada: " + jornadaId));

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
        List<StockAdjustment> adjustments = stockAdjustmentRepository.findByJornadaId(jornadaId);

        return new JornadaSummary(
                jornadaId,
                jornada.getOpenedAt(),
                jornada.getClosedAt(),
                jornada.isOpen(),
                totalSales,
                totalCost,
                totalExpenses,
                salesCount,
                adjustments
        );
    }

    @Override
    public PeriodSummary summary(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(BOGOTA_ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(BOGOTA_ZONE).toInstant();

        List<Sale> sales = saleRepository.findByPeriod(start, end);
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

        List<Expense> expenses = expenseRepository.findByPeriod(start, end);
        long totalExpenses = expenses.stream().mapToLong(Expense::getAmount).sum();

        return new PeriodSummary(from, to, totalSales, totalCost, totalExpenses, salesCount);
    }
}
