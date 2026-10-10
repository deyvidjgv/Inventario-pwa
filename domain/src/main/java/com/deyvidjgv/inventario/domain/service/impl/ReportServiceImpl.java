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

        List<Sale> productSales = saleRepository.findByProductId(productId).stream()
                .filter(s -> !s.isVoided())
                .collect(Collectors.toList());

        return calculateProductMargin(product, productSales);
    }

    @Override
    public List<ProductMarginReport> allMargins() {
        return productRepository.findActive().stream()
                .map(p -> margins(p.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public ProductMarginReport marginsForJornada(long productId, long jornadaId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND, "Producto no encontrado: " + productId));

        List<Sale> jornadaSales = saleRepository.findByJornadaId(jornadaId).stream()
                .filter(s -> !s.isVoided() && s.getProductId() == productId)
                .collect(Collectors.toList());

        return calculateProductMargin(product, jornadaSales);
    }

    @Override
    public List<ProductMarginReport> allMarginsForJornada(long jornadaId) {
        List<Sale> jornadaSales = saleRepository.findByJornadaId(jornadaId).stream()
                .filter(s -> !s.isVoided())
                .collect(Collectors.toList());

        Map<Long, List<Sale>> salesByProduct = jornadaSales.stream()
                .collect(Collectors.groupingBy(Sale::getProductId));

        List<ProductMarginReport> reports = new ArrayList<>();
        List<Product> activeProducts = productRepository.findActive();

        for (Product product : activeProducts) {
            List<Sale> pSales = salesByProduct.getOrDefault(product.getId(), Collections.emptyList());
            reports.add(calculateProductMargin(product, pSales));
        }

        // Ordenar: primero los que tienen ventas (mayor venta primero), luego el resto
        reports.sort((a, b) -> {
            int cmp = Long.compare(b.getTotalSales(), a.getTotalSales());
            if (cmp != 0) return cmp;
            return a.getProductName().compareToIgnoreCase(b.getProductName());
        });

        return reports;
    }

    private ProductMarginReport calculateProductMargin(Product product, List<Sale> sales) {
        List<StockLot> activeLots = stockLotRepository.findActiveByProductId(product.getId());
        List<LotMargin> lotMargins = activeLots.stream()
                .map(lot -> new LotMargin(lot.getId(), lot.getUnitCost(), lot.getQuantityRemaining(), product.getSalePrice()))
                .collect(Collectors.toList());

        Optional<StockLot> newestLot = stockLotRepository.findNewestByProductId(product.getId());
        Long newestLotCost = newestLot.map(StockLot::getUnitCost).orElse(null);

        int unitsSold = 0;
        long totalSales = 0;
        long realProfit = 0;
        long profitWithNewCost = 0;

        for (Sale sale : sales) {
            if (sale.isVoided()) continue;
            unitsSold += sale.getQuantity();
            totalSales += sale.getTotal();

            if (product.isTracksStock()) {
                List<SaleLotAllocation> allocations = saleRepository.findAllocationsBySaleId(sale.getId());
                for (SaleLotAllocation alloc : allocations) {
                    realProfit += (sale.getUnitPrice() - alloc.getUnitCost()) * alloc.getQuantity();
                    if (newestLotCost != null) {
                        profitWithNewCost += (sale.getUnitPrice() - newestLotCost) * alloc.getQuantity();
                    } else {
                        profitWithNewCost += (sale.getUnitPrice() - alloc.getUnitCost()) * alloc.getQuantity();
                    }
                }
            } else {
                realProfit += sale.getTotal();
                profitWithNewCost += sale.getTotal();
            }
        }

        long totalCost = totalSales - realProfit;

        return new ProductMarginReport(
                product.getId(),
                product.getName(),
                product.getSalePrice(),
                newestLotCost,
                lotMargins,
                realProfit,
                profitWithNewCost,
                unitsSold,
                totalSales,
                totalCost
        );
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

        List<Expense> expenses = new ArrayList<>(expenseRepository.findByJornadaId(jornadaId));
        Instant opened = jornada.getOpenedAt();
        Instant closed = jornada.getClosedAt() != null ? jornada.getClosedAt() : Instant.now();
        List<Expense> periodExpenses = expenseRepository.findByPeriod(opened, closed);
        Set<Long> alreadyIncludedIds = expenses.stream().map(Expense::getId).collect(Collectors.toSet());
        for (Expense pe : periodExpenses) {
            if (pe.getJornadaId() == null && !alreadyIncludedIds.contains(pe.getId())) {
                expenses.add(pe);
            }
        }

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

    @Override
    public com.deyvidjgv.inventario.domain.dto.DailyReport dailyReport(LocalDate date) {
        Instant start = date.atStartOfDay(BOGOTA_ZONE).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(BOGOTA_ZONE).toInstant();

        // 1. Ventas no anuladas en el día
        List<Sale> sales = saleRepository.findByPeriod(start, end).stream()
                .filter(s -> !s.isVoided())
                .collect(Collectors.toList());

        List<Long> saleIds = sales.stream().map(Sale::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, List<SaleLotAllocation>> allocsBySale = saleRepository.findAllocationsBySaleIds(saleIds).stream()
                .collect(Collectors.groupingBy(SaleLotAllocation::getSaleId));

        long totalSales = 0;
        long totalCost = 0;
        int salesCount = sales.size();

        Map<Long, String> productNames = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        Map<Long, List<Sale>> salesByProduct = sales.stream()
                .collect(Collectors.groupingBy(Sale::getProductId));

        List<com.deyvidjgv.inventario.domain.dto.ProductSaleDetail> productsSold = new ArrayList<>();

        for (Map.Entry<Long, List<Sale>> entry : salesByProduct.entrySet()) {
            long pId = entry.getKey();
            List<Sale> pSales = entry.getValue();
            String pName = productNames.getOrDefault(pId, "Producto #" + pId);

            int units = 0;
            long pTotalSales = 0;
            long pTotalCost = 0;

            for (Sale s : pSales) {
                units += s.getQuantity();
                pTotalSales += s.getTotal();
                List<SaleLotAllocation> allocs = allocsBySale.getOrDefault(s.getId(), Collections.emptyList());
                for (SaleLotAllocation a : allocs) {
                    pTotalCost += a.getTotalCost();
                }
            }

            long pProfit = pTotalSales - pTotalCost;
            totalSales += pTotalSales;
            totalCost += pTotalCost;

            productsSold.add(new com.deyvidjgv.inventario.domain.dto.ProductSaleDetail(pId, pName, units, pTotalSales, pTotalCost, pProfit));
        }

        productsSold.sort((a, b) -> Long.compare(b.getTotalSales(), a.getTotalSales()));

        // 2. Gastos en el día
        List<Expense> expenses = expenseRepository.findByPeriod(start, end);
        long totalExpenses = expenses.stream().mapToLong(Expense::getAmount).sum();

        // 3. Entradas de mercancía en el día (excluyendo ajustes de conteo físico)
        List<StockLot> lots = stockLotRepository.findByPeriod(start, end).stream()
                .filter(l -> l.getNote() == null || !l.getNote().startsWith("Ajuste conteo físico"))
                .collect(Collectors.toList());

        List<com.deyvidjgv.inventario.domain.dto.StockEntryDetail> stockEntries = new ArrayList<>();
        for (StockLot l : lots) {
            String pName = productNames.getOrDefault(l.getProductId(), "Producto #" + l.getProductId());
            stockEntries.add(new com.deyvidjgv.inventario.domain.dto.StockEntryDetail(
                    l.getId(),
                    l.getProductId(),
                    pName,
                    l.getQuantityIn(),
                    l.getUnitCost(),
                    l.getReceivedAt(),
                    l.getNote()
            ));
        }

        // 4. Ajustes / Mermas de inventario en el día
        List<StockAdjustment> adjustments = stockAdjustmentRepository.findByPeriod(start, end);

        List<com.deyvidjgv.inventario.domain.dto.StockAdjustmentDetail> adjustmentDetails = new ArrayList<>();
        for (StockAdjustment adj : adjustments) {
            String pName = productNames.getOrDefault(adj.getProductId(), "Producto #" + adj.getProductId());
            Optional<StockLot> newestLot = stockLotRepository.findNewestByProductId(adj.getProductId());
            long unitCost = newestLot.map(StockLot::getUnitCost).orElse(0L);
            long totalImpact = (long) adj.getDifference() * unitCost;

            adjustmentDetails.add(new com.deyvidjgv.inventario.domain.dto.StockAdjustmentDetail(
                    adj.getId(),
                    adj.getProductId(),
                    pName,
                    adj.getJornadaId(),
                    adj.getExpected(),
                    adj.getCounted(),
                    adj.getDifference(),
                    unitCost,
                    totalImpact,
                    adj.getCreatedAt()
            ));
        }

        return new com.deyvidjgv.inventario.domain.dto.DailyReport(
                date,
                totalSales,
                totalCost,
                totalExpenses,
                salesCount,
                productsSold,
                stockEntries,
                expenses,
                adjustmentDetails
        );
    }
}
