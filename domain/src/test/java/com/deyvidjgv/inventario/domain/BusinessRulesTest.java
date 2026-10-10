package com.deyvidjgv.inventario.domain;

import com.deyvidjgv.inventario.domain.dto.LotMargin;
import com.deyvidjgv.inventario.domain.dto.ProductMarginReport;
import com.deyvidjgv.inventario.domain.dto.ProductStock;
import com.deyvidjgv.inventario.domain.dto.JornadaSummary;
import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;
import com.deyvidjgv.inventario.domain.mock.InMemoryRepositories;
import com.deyvidjgv.inventario.domain.model.*;
import com.deyvidjgv.inventario.domain.service.*;
import com.deyvidjgv.inventario.domain.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Casos de Prueba Obligatorios de Lógica de Negocio")
class BusinessRulesTest {

    private InMemoryRepositories.CategoryRepositoryImpl categoryRepository;
    private InMemoryRepositories.ProductRepositoryImpl productRepository;
    private InMemoryRepositories.StockLotRepositoryImpl stockLotRepository;
    private InMemoryRepositories.JornadaRepositoryImpl jornadaRepository;
    private InMemoryRepositories.SaleRepositoryImpl saleRepository;
    private InMemoryRepositories.StockAdjustmentRepositoryImpl stockAdjustmentRepository;
    private InMemoryRepositories.ExpenseRepositoryImpl expenseRepository;
    private InMemoryRepositories.AuditLogRepositoryImpl auditLogRepository;
    private InMemoryRepositories.TransactionManagerImpl transactionManager;

    private InventoryService inventoryService;
    private SalesService salesService;
    private ReportService reportService;
    private ExpenseService expenseService;

    private Category cervezasCategory;
    private Category juegosCategory;

    @BeforeEach
    void setUp() {
        categoryRepository = new InMemoryRepositories.CategoryRepositoryImpl();
        productRepository = new InMemoryRepositories.ProductRepositoryImpl();
        stockLotRepository = new InMemoryRepositories.StockLotRepositoryImpl();
        jornadaRepository = new InMemoryRepositories.JornadaRepositoryImpl();
        saleRepository = new InMemoryRepositories.SaleRepositoryImpl();
        stockAdjustmentRepository = new InMemoryRepositories.StockAdjustmentRepositoryImpl();
        expenseRepository = new InMemoryRepositories.ExpenseRepositoryImpl();
        auditLogRepository = new InMemoryRepositories.AuditLogRepositoryImpl();
        transactionManager = new InMemoryRepositories.TransactionManagerImpl();

        inventoryService = new InventoryServiceImpl(productRepository, stockLotRepository, categoryRepository);
        salesService = new SalesServiceImpl(
                jornadaRepository,
                productRepository,
                stockLotRepository,
                saleRepository,
                stockAdjustmentRepository,
                expenseRepository,
                auditLogRepository,
                transactionManager
        );
        reportService = new ReportServiceImpl(
                productRepository,
                stockLotRepository,
                saleRepository,
                jornadaRepository,
                expenseRepository,
                stockAdjustmentRepository
        );
        expenseService = new ExpenseServiceImpl(expenseRepository, jornadaRepository);

        cervezasCategory = categoryRepository.save(new Category("Cervezas"));
        juegosCategory = categoryRepository.save(new Category("Juegos"));
    }

    @Test
    @DisplayName("Caso A: Dos lotes (5.000 y 4.300), venta de 30 a 6.000 -> ganancia 34.200 y diferencia 16.800")
    void testCaseA_TwoLotsAndCrossSale() {
        Instant t0 = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant t1 = Instant.now().minus(1, ChronoUnit.HOURS);

        // Crear producto Águila a 6.000
        Product aguila = inventoryService.createProduct("Cerveza Águila", cervezasCategory.getId(), 6000L, true);

        // Lote 1: 24 unidades a 5.000
        StockLot lote1 = inventoryService.receiveStock(aguila.getId(), 24, 5000L, t0, "Compra 1");
        // Lote 2: 24 unidades a 4.300
        StockLot lote2 = inventoryService.receiveStock(aguila.getId(), 24, 4300L, t1, "Compra 2");

        // Abrir jornada
        salesService.openJornada(Instant.now());

        // Vender 30 unidades a 6.000
        Sale sale = salesService.sell(aguila.getId(), 30);
        assertEquals(180000L, sale.getTotal());

        // Verificar asignación FIFO
        List<SaleLotAllocation> allocations = saleRepository.findAllocationsBySaleId(sale.getId());
        assertEquals(2, allocations.size());

        SaleLotAllocation alloc1 = allocations.get(0);
        assertEquals(lote1.getId(), alloc1.getLotId());
        assertEquals(24, alloc1.getQuantity());
        assertEquals(5000L, alloc1.getUnitCost());

        SaleLotAllocation alloc2 = allocations.get(1);
        assertEquals(lote2.getId(), alloc2.getLotId());
        assertEquals(6, alloc2.getQuantity());
        assertEquals(4300L, alloc2.getUnitCost());

        // Ganancia real = 24 * (6.000 - 5.000) + 6 * (6.000 - 4.300) = 24.000 + 10.200 = 34.200
        ProductMarginReport report = reportService.margins(aguila.getId());
        assertEquals(34200L, report.getRealProfit(), "La ganancia real debe ser 34.200");

        // Con costo nuevo (4.300) = 30 * (6.000 - 4.300) = 51.000
        assertEquals(51000L, report.getProfitWithNewCost(), "La ganancia con costo nuevo debe ser 51.000");

        // Diferencia = 51.000 - 34.200 = 16.800
        assertEquals(16800L, report.getDifference(), "La diferencia debe ser 16.800");

        // Verificar márgenes de los lotes
        // Lote 1: precio 6.000, costo 5.000 -> margen venta 16.7%, margen costo 20.0%
        LotMargin m1 = new LotMargin(lote1.getId(), 5000L, 0, 6000L);
        assertEquals(16.7, m1.getMarginOnSale(), 0.1);
        assertEquals(20.0, m1.getMarginOnCost(), 0.1);

        // Lote 2: precio 6.000, costo 4.300 -> margen venta 28.3%, margen costo 39.5%
        LotMargin m2 = new LotMargin(lote2.getId(), 4300L, 18, 6000L);
        assertEquals(28.3, m2.getMarginOnSale(), 0.1);
        assertEquals(39.5, m2.getMarginOnCost(), 0.1);
    }

    @Test
    @DisplayName("Caso B: Productos de pool separados y juego sin stock")
    void testCaseB_PoolProductsSeparated() {
        Product aguilaPool = inventoryService.createProduct("Cerveza Águila Pool", cervezasCategory.getId(), 8000L, true);
        Product juegoPool = inventoryService.createProduct("Juego Pool", juegosCategory.getId(), 2000L, false);

        // Cerveza Águila Pool con lote a 4.300
        StockLot lotePool = inventoryService.receiveStock(aguilaPool.getId(), 10, 4300L, Instant.now(), "Lote pool");

        salesService.openJornada(Instant.now());

        // Vender 1 cerveza pool
        Sale saleCerveza = salesService.sell(aguilaPool.getId(), 1);
        List<SaleLotAllocation> allocsCerveza = saleRepository.findAllocationsBySaleId(saleCerveza.getId());
        assertEquals(1, allocsCerveza.size());
        assertEquals(4300L, allocsCerveza.get(0).getUnitCost());
        // Ganancia por unidad = 8.000 - 4.300 = 3.700
        assertEquals(3700L, saleCerveza.getUnitPrice() - allocsCerveza.get(0).getUnitCost());

        // Vender 1 juego pool
        Sale saleJuego = salesService.sell(juegoPool.getId(), 1);
        List<SaleLotAllocation> allocsJuego = saleRepository.findAllocationsBySaleId(saleJuego.getId());
        assertTrue(allocsJuego.isEmpty(), "Juego sin stock no debe generar asignaciones de lotes");
        assertEquals(2000L, saleJuego.getTotal());

        // Ganancia de juego pool es completa (costo 0)
        ProductMarginReport reportJuego = reportService.margins(juegoPool.getId());
        assertEquals(2000L, reportJuego.getRealProfit());
    }

    @Test
    @DisplayName("Caso C: Anular una venta devuelve las unidades a sus lotes originales")
    void testCaseC_VoidSaleRestoresLots() {
        Product aguila = inventoryService.createProduct("Cerveza Águila", cervezasCategory.getId(), 6000L, true);
        StockLot lote1 = inventoryService.receiveStock(aguila.getId(), 24, 5000L, Instant.now().minus(2, ChronoUnit.HOURS), "Lote 1");
        StockLot lote2 = inventoryService.receiveStock(aguila.getId(), 24, 4300L, Instant.now().minus(1, ChronoUnit.HOURS), "Lote 2");

        salesService.openJornada(Instant.now());
        Sale sale = salesService.sell(aguila.getId(), 30);

        // Verificar que disminuyó el stock
        assertEquals(0, stockLotRepository.findById(lote1.getId()).get().getQuantityRemaining());
        assertEquals(18, stockLotRepository.findById(lote2.getId()).get().getQuantityRemaining());

        // Anular la venta
        salesService.voidSale(sale.getId());

        // Comprobar reversión íntegra de lotes
        StockLot lote1Restaurado = stockLotRepository.findById(lote1.getId()).get();
        StockLot lote2Restaurado = stockLotRepository.findById(lote2.getId()).get();
        assertEquals(24, lote1Restaurado.getQuantityRemaining(), "Lote 1 debe volver a 24");
        assertEquals(24, lote2Restaurado.getQuantityRemaining(), "Lote 2 debe volver a 24");

        // Comprobar estado de la venta y auditoría
        Sale saleAnulada = saleRepository.findById(sale.getId()).get();
        assertTrue(saleAnulada.isVoided());

        List<AuditLog> logs = auditLogRepository.findByEntity("Sale", sale.getId());
        assertEquals(1, logs.size());
        assertEquals("VOID", logs.get(0).getAction());
    }

    @Test
    @DisplayName("Caso D: Stock insuficiente se rechaza y no altera nada")
    void testCaseD_InsufficientStockRejectsSale() {
        Product aguila = inventoryService.createProduct("Cerveza Águila", cervezasCategory.getId(), 6000L, true);
        inventoryService.receiveStock(aguila.getId(), 24, 5000L, Instant.now(), "Lote 1");
        inventoryService.receiveStock(aguila.getId(), 24, 4300L, Instant.now(), "Lote 2");
        // Total = 48 unidades

        salesService.openJornada(Instant.now());

        DomainException exception = assertThrows(DomainException.class, () -> {
            salesService.sell(aguila.getId(), 49);
        });

        assertEquals(ErrorCode.INSUFFICIENT_STOCK, exception.getErrorCode());

        // Verificar que no se realizaron ventas ni se alteró el stock
        assertTrue(saleRepository.findAll().isEmpty());
        List<ProductStock> stocks = inventoryService.listStock();
        assertEquals(48, stocks.get(0).getCurrentStock());
    }

    @Test
    @DisplayName("Caso E: Conteo al cerrar con faltante (-2) registra pérdida y ajusta lotes")
    void testCaseE_CountWithShortageAdjustsStock() {
        Product aguila = inventoryService.createProduct("Cerveza Águila", cervezasCategory.getId(), 6000L, true);
        inventoryService.receiveStock(aguila.getId(), 24, 5000L, Instant.now().minus(2, ChronoUnit.HOURS), "Lote 1");
        StockLot lote2 = inventoryService.receiveStock(aguila.getId(), 24, 4300L, Instant.now().minus(1, ChronoUnit.HOURS), "Lote 2");

        Jornada jornada = salesService.openJornada(Instant.now());
        salesService.sell(aguila.getId(), 30);
        // Quedan 18 en lote 2

        // Conteo físico: se cuentan 16
        Map<Long, Integer> counted = new HashMap<>();
        counted.put(aguila.getId(), 16);

        JornadaSummary summary = salesService.closeJornada(jornada.getId(), Instant.now(), counted);

        // Verificar ajuste registrado
        assertEquals(1, summary.getAdjustments().size());
        StockAdjustment adj = summary.getAdjustments().get(0);
        assertEquals(18, adj.getExpected());
        assertEquals(16, adj.getCounted());
        assertEquals(-2, adj.getDifference());

        // Pérdida al costo: 2 * 4.300 = 8.600
        long perdidaAlCosto = Math.abs(adj.getDifference()) * lote2.getUnitCost();
        assertEquals(8600L, perdidaAlCosto);

        // Pérdida al precio de venta: 2 * 6.000 = 12.000
        long perdidaAlPrecioVenta = Math.abs(adj.getDifference()) * aguila.getSalePrice();
        assertEquals(12000L, perdidaAlPrecioVenta);

        // Comprobar que en stock quedan 16
        StockLot lote2Actualizado = stockLotRepository.findById(lote2.getId()).get();
        assertEquals(16, lote2Actualizado.getQuantityRemaining());
    }

    @Test
    @DisplayName("Caso F: Cambio de precio posterior no altera ventas viejas")
    void testCaseF_PriceChangeDoesNotAlterOldSales() {
        Product aguila = inventoryService.createProduct("Cerveza Águila", cervezasCategory.getId(), 6000L, true);
        inventoryService.receiveStock(aguila.getId(), 50, 4000L, Instant.now(), "Lote 1");

        salesService.openJornada(Instant.now());

        // Primera venta a 6.000
        Sale sale1 = salesService.sell(aguila.getId(), 5);
        assertEquals(6000L, sale1.getUnitPrice());
        assertEquals(30000L, sale1.getTotal());

        // Subir precio a 6.500
        Product aguilaActualizada = aguila.withSalePrice(6500L);
        inventoryService.updateProduct(aguilaActualizada);

        // Segunda venta a 6.500
        Sale sale2 = salesService.sell(aguila.getId(), 5);
        assertEquals(6500L, sale2.getUnitPrice());
        assertEquals(32500L, sale2.getTotal());

        // Comprobar inmutabilidad de la primera venta
        Sale sale1Recuperada = saleRepository.findById(sale1.getId()).get();
        assertEquals(6000L, sale1Recuperada.getUnitPrice(), "El precio histórico de la venta 1 debe mantenerse en 6.000");
        assertEquals(30000L, sale1Recuperada.getTotal());
    }

    @Test
    @DisplayName("Caso G: Una sola jornada abierta y ventas bloqueadas sin jornada")
    void testCaseG_SingleOpenJornadaAndBlockedSales() {
        Product aguila = inventoryService.createProduct("Cerveza Águila", cervezasCategory.getId(), 6000L, true);
        inventoryService.receiveStock(aguila.getId(), 10, 4000L, Instant.now(), "Lote");

        // Intentar vender sin jornada abierta -> debe fallar
        DomainException exVenta = assertThrows(DomainException.class, () -> {
            salesService.sell(aguila.getId(), 1);
        });
        assertEquals(ErrorCode.NO_OPEN_JORNADA, exVenta.getErrorCode());

        // Abrir primera jornada
        Jornada jornada1 = salesService.openJornada(Instant.now());
        assertTrue(jornada1.isOpen());

        // Intentar abrir una segunda jornada simultánea -> debe fallar
        DomainException exJornada = assertThrows(DomainException.class, () -> {
            salesService.openJornada(Instant.now());
        });
        assertEquals(ErrorCode.JORNADA_ALREADY_OPEN, exJornada.getErrorCode());

        // Cerrar jornada 1
        salesService.closeJornada(jornada1.getId(), Instant.now(), Collections.emptyMap());

        // Reabrir última jornada
        Jornada reabierta = salesService.reopenLastJornada();
        assertEquals(jornada1.getId(), reabierta.getId());
        assertTrue(reabierta.isOpen());
    }

    @Test
    @DisplayName("Resumen de jornada con ventas, costos y gastos independientes")
    void testJornadaSummaryWithExpenses() {
        Product aguila = inventoryService.createProduct("Cerveza Águila", cervezasCategory.getId(), 6000L, true);
        inventoryService.receiveStock(aguila.getId(), 24, 5000L, Instant.now().minus(2, ChronoUnit.HOURS), "Lote 1");
        inventoryService.receiveStock(aguila.getId(), 24, 4300L, Instant.now().minus(1, ChronoUnit.HOURS), "Lote 2");

        Jornada jornada = salesService.openJornada(Instant.now());
        salesService.sell(aguila.getId(), 30); // Ventas: 180.000, Costo: 145.800, Ganancia: 34.200

        // Registrar un gasto libre (hielo a 20.000)
        expenseService.add("Hielo para el bar", 20000L, Instant.now(), jornada.getId());

        JornadaSummary summary = reportService.summary(jornada.getId());
        assertEquals(180000L, summary.getTotalSales());
        assertEquals(145800L, summary.getTotalCost());
        assertEquals(34200L, summary.getGrossProfit(), "La ganancia bruta de productos no debe ser alterada por los gastos");
        assertEquals(20000L, summary.getTotalExpenses());
        assertEquals(14200L, summary.getNetInformativeProfit(), "Ganancia neta informativa = 34.200 - 20.000 = 14.200");
    }

    @Test
    @DisplayName("Caso H: Exportar, borrar todo e importar deja los mismos datos y totales")
    void testCaseH_BackupRoundtrip() {
        BackupService backupService = new BackupServiceImpl(
                categoryRepository,
                productRepository,
                stockLotRepository,
                jornadaRepository,
                saleRepository,
                stockAdjustmentRepository,
                expenseRepository,
                auditLogRepository,
                transactionManager
        );

        Product aguila = inventoryService.createProduct("Cerveza Águila", cervezasCategory.getId(), 6000L, true);
        inventoryService.receiveStock(aguila.getId(), 24, 5000L, Instant.now().minus(2, ChronoUnit.HOURS), "Lote 1");
        inventoryService.receiveStock(aguila.getId(), 24, 4300L, Instant.now().minus(1, ChronoUnit.HOURS), "Lote 2");

        Jornada jornada = salesService.openJornada(Instant.now());
        salesService.sell(aguila.getId(), 30);
        expenseService.add("Hielo", 20000L, Instant.now(), jornada.getId());

        // Exportar a JSON
        String backupJson = backupService.exportJson();
        assertNotNull(backupJson);
        assertTrue(backupJson.contains("Cerveza Águila"));

        // Guardar métricas antes del borrado
        JornadaSummary summaryBefore = reportService.summary(jornada.getId());
        ProductMarginReport marginBefore = reportService.margins(aguila.getId());

        // Importar reemplazando todo
        backupService.importJson(backupJson);

        // Validar que los datos y métricas sean idénticos
        JornadaSummary summaryAfter = reportService.summary(jornada.getId());
        assertEquals(summaryBefore.getTotalSales(), summaryAfter.getTotalSales());
        assertEquals(summaryBefore.getTotalCost(), summaryAfter.getTotalCost());
        assertEquals(summaryBefore.getGrossProfit(), summaryAfter.getGrossProfit());
        assertEquals(summaryBefore.getTotalExpenses(), summaryAfter.getTotalExpenses());
        assertEquals(summaryBefore.getNetInformativeProfit(), summaryAfter.getNetInformativeProfit());

        ProductMarginReport marginAfter = reportService.margins(aguila.getId());
        assertEquals(marginBefore.getRealProfit(), marginAfter.getRealProfit());
        assertEquals(marginBefore.getProfitWithNewCost(), marginAfter.getProfitWithNewCost());
        assertEquals(marginBefore.getDifference(), marginAfter.getDifference());
    }

    @Test
    @DisplayName("Reporte Diario: Entradas de mercancía, salidas, gastos, ventas y ganancia neta por fecha")
    void testDailyReport() {
        LocalDate today = LocalDate.now();

        // 1. Crear producto y entrar mercancía hoy
        Product poker = inventoryService.createProduct("Póker 330ml", cervezasCategory.getId(), 5000L, true);
        inventoryService.receiveStock(poker.getId(), 20, 3000L, Instant.now(), "Compra Bavaria");

        // 2. Abrir jornada y vender 5 unidades
        salesService.openJornada(Instant.now());
        salesService.sell(poker.getId(), 5); // Total: 25.000, Costo: 15.000, Ganancia: 10.000

        // 3. Registrar gasto hoy
        expenseService.add("Hielo y bolsas", 4000L, Instant.now(), null);

        // 4. Consultar reporte diario
        com.deyvidjgv.inventario.domain.dto.DailyReport report = reportService.dailyReport(today);

        assertEquals(today, report.getDate());
        assertEquals(25000L, report.getTotalSales());
        assertEquals(15000L, report.getTotalCost());
        assertEquals(10000L, report.getGrossProfit());
        assertEquals(4000L, report.getTotalExpenses());
        assertEquals(6000L, report.getNetProfit());
        assertEquals(1, report.getSalesCount());

        // Verificar productos vendidos
        assertEquals(1, report.getProductsSold().size());
        com.deyvidjgv.inventario.domain.dto.ProductSaleDetail saleDetail = report.getProductsSold().get(0);
        assertEquals("Póker 330ml", saleDetail.getProductName());
        assertEquals(5, saleDetail.getUnitsSold());
        assertEquals(25000L, saleDetail.getTotalSales());
        assertEquals(15000L, saleDetail.getTotalCost());
        assertEquals(10000L, saleDetail.getRealProfit());

        // Verificar entradas de mercancía
        assertEquals(1, report.getStockEntries().size());
        com.deyvidjgv.inventario.domain.dto.StockEntryDetail entry = report.getStockEntries().get(0);
        assertEquals("Póker 330ml", entry.getProductName());
        assertEquals(20, entry.getQuantity());
        assertEquals(3000L, entry.getUnitCost());
        assertEquals(60000L, entry.getTotalInvestment());
        assertEquals("Compra Bavaria", entry.getNote());

        // Verificar gastos
        assertEquals(1, report.getExpenses().size());
        assertEquals("Hielo y bolsas", report.getExpenses().get(0).getConcept());
        assertEquals(4000L, report.getExpenses().get(0).getAmount());
    }
}
