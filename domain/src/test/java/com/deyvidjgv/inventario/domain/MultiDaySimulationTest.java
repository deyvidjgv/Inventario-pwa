package com.deyvidjgv.inventario.domain;

import com.deyvidjgv.inventario.domain.dto.*;
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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Simulación Realista Multi-Día de Operación Bar & Billar Pool")
public class MultiDaySimulationTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

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
    private BackupService backupService;

    // Categorías y Productos
    private Category catCervezas;
    private Category catLicores;
    private Category catSnacks;
    private Category catJuegos;

    private Product aguila;
    private Product poker;
    private Product aguardiente;
    private Product papas;
    private Product juegoPool;

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
        backupService = new BackupServiceImpl(
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

        // Crear categorías
        catCervezas = inventoryService.createCategory("Cervezas");
        catLicores = inventoryService.createCategory("Licores");
        catSnacks = inventoryService.createCategory("Snacks");
        catJuegos = inventoryService.createCategory("Juegos y Servicios");

        // Crear productos
        aguila = inventoryService.createProduct("Cerveza Águila 330ml", catCervezas.getId(), 6000L, true);
        poker = inventoryService.createProduct("Cerveza Poker 330ml", catCervezas.getId(), 5500L, true);
        aguardiente = inventoryService.createProduct("Aguardiente Antioqueño 750ml", catLicores.getId(), 90000L, true);
        papas = inventoryService.createProduct("Papas Fritas Margarita", catSnacks.getId(), 3500L, true);
        juegoPool = inventoryService.createProduct("Ficha / Hora Pool", catJuegos.getId(), 3000L, false);
    }

    private Instant instantAt(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(BOGOTA).toInstant();
    }

    @Test
    @DisplayName("Día 1: Apertura, Entradas de Lotes, Ventas, Anulación, Gastos y Arqueo Cuadrado")
    void testDay1_StandardOperationsAndReportReconciliation() {
        LocalDate day1 = LocalDate.of(2026, 10, 1);

        // 1. Entradas de inventario inicial
        inventoryService.receiveStock(aguila.getId(), 50, 4000L, instantAt(day1, 10, 0), "Lote Inicial Águila");
        inventoryService.receiveStock(poker.getId(), 40, 3800L, instantAt(day1, 10, 15), "Lote Inicial Poker");
        inventoryService.receiveStock(aguardiente.getId(), 10, 65000L, instantAt(day1, 10, 30), "Lote Aguardiente");
        inventoryService.receiveStock(papas.getId(), 30, 2200L, instantAt(day1, 10, 45), "Lote Papas");

        // 2. Abrir turno 1 de la tarde (2:00 PM)
        Jornada shift1 = salesService.openJornada(instantAt(day1, 14, 0));
        assertTrue(shift1.isOpen());

        // 3. Gastos del turno
        expenseService.add("Hielo en bolsa", 15000L, instantAt(day1, 14, 30), shift1.getId());
        expenseService.add("Limones", 5000L, instantAt(day1, 14, 35), shift1.getId());

        // 4. Ventas
        // 10 Águilas (10 * 6.000 = 60.000; costo = 10 * 4.000 = 40.000; ganancia = 20.000)
        Sale saleAguila = salesService.sell(aguila.getId(), 10, instantAt(day1, 15, 0));
        assertEquals(60000L, saleAguila.getTotal());

        // 5 Poker (5 * 5.500 = 27.500; costo = 5 * 3.800 = 19.000; ganancia = 8.500)
        Sale salePoker = salesService.sell(poker.getId(), 5, instantAt(day1, 15, 30));
        assertEquals(27500L, salePoker.getTotal());

        // 4 Fichas Pool (4 * 3.000 = 12.000; costo = 0; ganancia = 12.000)
        Sale salePool = salesService.sell(juegoPool.getId(), 4, instantAt(day1, 16, 0));
        assertEquals(12000L, salePool.getTotal());

        // Venta por error: 2 Aguardientes (2 * 90.000 = 180.000)
        Sale accidentalSale = salesService.sell(aguardiente.getId(), 2, instantAt(day1, 17, 0));
        assertEquals(180000L, accidentalSale.getTotal());
        assertEquals(8, stockLotRepository.findActiveByProductId(aguardiente.getId()).get(0).getQuantityRemaining());

        // 5. Anular venta por error
        salesService.voidSale(accidentalSale.getId());
        // Se restauran los 2 aguardientes al stock original
        assertEquals(10, stockLotRepository.findActiveByProductId(aguardiente.getId()).get(0).getQuantityRemaining());

        // 6. Cierre de turno a las 10:00 PM con conteo físico exacto
        Map<Long, Integer> counted = new HashMap<>();
        counted.put(aguila.getId(), 40); // 50 - 10 = 40
        counted.put(poker.getId(), 35);  // 40 - 5 = 35
        counted.put(aguardiente.getId(), 10);
        counted.put(papas.getId(), 30);

        JornadaSummary summary1 = salesService.closeJornada(shift1.getId(), instantAt(day1, 22, 0), counted);

        // Verificaciones de Turno 1
        assertEquals(99500L, summary1.getTotalSales(), "Ventas: 60k + 27.5k + 12k = 99.500");
        assertEquals(59000L, summary1.getTotalCost(), "Costo: 40k + 19k + 0 = 59.000");
        assertEquals(40500L, summary1.getGrossProfit(), "Ganancia Bruta: 99.500 - 59.000 = 40.500");
        assertEquals(20000L, summary1.getTotalExpenses(), "Gastos: 15k + 5k = 20.000");
        assertEquals(20500L, summary1.getNetInformativeProfit(), "Ganancia Neta: 40.500 - 20.000 = 20.500");
        assertTrue(summary1.getAdjustments().isEmpty(), "No hubo descuadres en conteo");

        // 7. Verificación del Reporte Diario del Día 1
        DailyReport reportDay1 = reportService.dailyReport(day1);
        assertEquals(day1, reportDay1.getDate());
        assertEquals(99500L, reportDay1.getTotalSales());
        assertEquals(59000L, reportDay1.getTotalCost());
        assertEquals(40500L, reportDay1.getGrossProfit());
        assertEquals(20000L, reportDay1.getTotalExpenses());
        assertEquals(20500L, reportDay1.getNetProfit());
        assertEquals(3, reportDay1.getSalesCount(), "3 ventas efectivas (la anulada no cuenta)");
        assertEquals(4, reportDay1.getStockEntries().size(), "4 entradas de mercancía recibidas");
        assertEquals(2, reportDay1.getExpenses().size(), "2 gastos registrados");
    }

    @Test
    @DisplayName("Día 2: Turno que Cruza la Medianoche, Trans-Lote FIFO y Arqueo con Mermas y Sobrantes")
    void testDay2_MidnightSpanningShiftAndStockAdjustments() {
        LocalDate day2 = LocalDate.of(2026, 10, 2);
        LocalDate day3 = LocalDate.of(2026, 10, 3);

        // Stock previo de Día 1:
        // Águila: 40 unidades a 4.000
        inventoryService.receiveStock(aguila.getId(), 40, 4000L, instantAt(day2, 12, 0), "Stock Restante Día 1");
        // Nuevo Lote de Águila: 30 unidades a 4.300 (Aumento de costo de Bavaria!)
        StockLot newAguilaLot = inventoryService.receiveStock(aguila.getId(), 30, 4300L, instantAt(day2, 17, 0), "Nuevo Lote Bavaria");

        // Poker: 20 unidades a 3.800
        inventoryService.receiveStock(poker.getId(), 20, 3800L, instantAt(day2, 12, 0), "Stock Restante Poker");

        // 1. Abrir Turno 2 a las 8:00 PM del Día 2
        Jornada shift2 = salesService.openJornada(instantAt(day2, 20, 0));

        // 2. Ventas antes de la medianoche (Día 2, 22:30)
        // Vender 30 Águilas -> Salen del Lote 1 (a 4.000). Quedan 10 en Lote 1 y 30 en Lote 2.
        salesService.sell(aguila.getId(), 30, instantAt(day2, 22, 30));

        // 3. Ventas después de la medianoche (Día 3, 01:15 AM)
        // Vender 25 Águilas -> Salen 10 del Lote 1 (a 4.000) y 15 del Lote 2 (a 4.300) [Cruce FIFO!]
        salesService.sell(aguila.getId(), 25, instantAt(day3, 1, 15));

        // 4. Gasto en la madrugada (Día 3, 02:00 AM)
        expenseService.add("Taxi de seguridad noche", 25000L, instantAt(day3, 2, 0), shift2.getId());

        // 5. Arqueo físico al cerrar el turno a las 3:00 AM del Día 3
        // En sistema para Águila deberían quedar: (40 + 30) - 55 = 15 unidades (todas del Lote 2).
        // Conteo real: Se cuentan 13 unidades (Hubo 2 botellas quebradas/faltantes!).
        // Poker: Deberían quedar 20, se cuentan 21 (sobró 1 unidad).
        Map<Long, Integer> counted = new HashMap<>();
        counted.put(aguila.getId(), 13);
        counted.put(poker.getId(), 21);

        JornadaSummary summary2 = salesService.closeJornada(shift2.getId(), instantAt(day3, 3, 0), counted);

        // Verificar Ajustes de Inventario del turno
        assertEquals(2, summary2.getAdjustments().size());
        StockAdjustment adjAguila = summary2.getAdjustments().stream().filter(a -> a.getProductId() == aguila.getId()).findFirst().get();
        assertEquals(15, adjAguila.getExpected());
        assertEquals(13, adjAguila.getCounted());
        assertEquals(-2, adjAguila.getDifference());

        StockAdjustment adjPoker = summary2.getAdjustments().stream().filter(a -> a.getProductId() == poker.getId()).findFirst().get();
        assertEquals(20, adjPoker.getExpected());
        assertEquals(21, adjPoker.getCounted());
        assertEquals(1, adjPoker.getDifference());

        // El stock en lote 2 de Águila debe haber quedado ajustado en 13
        assertEquals(13, stockLotRepository.findById(newAguilaLot.getId()).get().getQuantityRemaining());

        // Comprobar margen trans-lote de Águila:
        // Total vendidas: 55 unidades
        // Ventas totales = 55 * 6.000 = 330.000
        // Costo = (40 * 4.000) + (15 * 4.300) = 160.000 + 64.500 = 224.500
        // Ganancia Real = 330.000 - 224.500 = 105.500
        ProductMarginReport marginAguila = reportService.margins(aguila.getId());
        assertEquals(330000L, marginAguila.getTotalSales());
        assertEquals(224500L, marginAguila.getTotalCost());
        assertEquals(105500L, marginAguila.getRealProfit());

        // Con costo nuevo (4.300): 55 * (6.000 - 4.300) = 55 * 1.700 = 93.500
        assertEquals(93500L, marginAguila.getProfitWithNewCost());
        // Diferencia = 93.500 - 105.500 = -12.000
        assertEquals(-12000L, marginAguila.getDifference());

        // 6. Verificar separación en Reportes Diarios:
        // Reporte Día 2 (Ventas pre-medianoche: 30 Águilas a 6.000 = 180.000)
        DailyReport reportDay2 = reportService.dailyReport(day2);
        assertEquals(180000L, reportDay2.getTotalSales());
        assertEquals(120000L, reportDay2.getTotalCost());
        assertEquals(60000L, reportDay2.getGrossProfit());
        assertEquals(0L, reportDay2.getTotalExpenses());
        assertEquals(60000L, reportDay2.getNetProfit());

        // Reporte Día 3 (Ventas post-medianoche: 25 Águilas a 6.000 = 150.000)
        DailyReport reportDay3PostMidnight = reportService.dailyReport(day3);
        assertEquals(150000L, reportDay3PostMidnight.getTotalSales());
        assertEquals(104500L, reportDay3PostMidnight.getTotalCost());
        assertEquals(45500L, reportDay3PostMidnight.getGrossProfit());
        assertEquals(25000L, reportDay3PostMidnight.getTotalExpenses());
        assertEquals(20500L, reportDay3PostMidnight.getNetProfit());
        assertEquals(2, reportDay3PostMidnight.getAdjustments().size(), "Las 2 mermas del cierre a las 3 AM aparecen en el Día 3");
        StockAdjustmentDetail adjDetailAguila = reportDay3PostMidnight.getAdjustments().stream()
                .filter(a -> a.getProductId() == aguila.getId()).findFirst().get();
        assertEquals("Cerveza Águila 330ml", adjDetailAguila.getProductName(), "El nombre del producto debe estar presente");
        assertEquals(4300L, adjDetailAguila.getUnitCost());
        assertEquals(-8600L, adjDetailAguila.getTotalLossOrGainAtCost(), "Pérdida económica calculada al costo del lote más reciente: -2 * 4.300 = -8.600");
        assertEquals(8600L, reportDay3PostMidnight.getTotalShrinkageLoss(), "Pérdida total por mermas en el día = 8.600");
    }

    @Test
    @DisplayName("Día 3: Múltiples Turnos en un Mismo Día y Cambio de Precio a Mitad de Operación")
    void testDay3_MultipleShiftsInOneDayAndPriceChange() {
        LocalDate day3 = LocalDate.of(2026, 10, 3);

        inventoryService.receiveStock(poker.getId(), 50, 3800L, instantAt(day3, 10, 0), "Stock Poker");

        // Turno 1 (Tarde: 11:00 AM a 5:00 PM)
        Jornada shiftMorning = salesService.openJornada(instantAt(day3, 11, 0));
        // Venta de 10 Poker al precio original de 5.500
        Sale s1 = salesService.sell(poker.getId(), 10, instantAt(day3, 12, 0));
        assertEquals(55000L, s1.getTotal());
        salesService.closeJornada(shiftMorning.getId(), instantAt(day3, 17, 0), Collections.emptyMap());

        // A las 5:30 PM el dueño sube el precio de la Poker a 6.000
        Product updatedPoker = poker.withSalePrice(6000L);
        inventoryService.updateProduct(updatedPoker);

        // Turno 2 (Noche: 6:00 PM a 11:30 PM)
        Jornada shiftNight = salesService.openJornada(instantAt(day3, 18, 0));
        // Venta de 15 Poker al NUEVO precio de 6.000
        Sale s2 = salesService.sell(poker.getId(), 15, instantAt(day3, 19, 0));
        assertEquals(90000L, s2.getTotal());
        salesService.closeJornada(shiftNight.getId(), instantAt(day3, 23, 30), Collections.emptyMap());

        // Comprobar inmutabilidad histórica
        Sale s1Fetched = saleRepository.findById(s1.getId()).get();
        assertEquals(5500L, s1Fetched.getUnitPrice(), "El precio de la venta 1 debe seguir siendo 5.500");
        assertEquals(55000L, s1Fetched.getTotal());

        Sale s2Fetched = saleRepository.findById(s2.getId()).get();
        assertEquals(6000L, s2Fetched.getUnitPrice(), "El precio de la venta 2 debe ser 6.000");
        assertEquals(90000L, s2Fetched.getTotal());

        // Reporte Diario del Día 3 debe agrupar AMBOS turnos:
        // Ventas totales = 55.000 + 90.000 = 145.000
        // Unidades vendidas = 25
        // Costo = 25 * 3.800 = 95.000
        // Ganancia Real = 145.000 - 95.000 = 50.000
        DailyReport reportDay3 = reportService.dailyReport(day3);
        assertEquals(145000L, reportDay3.getTotalSales());
        assertEquals(95000L, reportDay3.getTotalCost());
        assertEquals(50000L, reportDay3.getGrossProfit());
        assertEquals(2, reportDay3.getSalesCount());
    }

    @Test
    @DisplayName("Día 4: Casos de Borde, Validaciones Robustas y Rechazo de Datos Inválidos")
    void testDay4_EdgeCasesAndInputValidation() {
        LocalDate day4 = LocalDate.of(2026, 10, 4);

        // 1. Venta sin turno abierto debe fallar
        DomainException exNoOpen = assertThrows(DomainException.class, () -> salesService.sell(aguila.getId(), 1));
        assertEquals(ErrorCode.NO_OPEN_JORNADA, exNoOpen.getErrorCode());

        // 2. Abrir turno
        Jornada shift = salesService.openJornada(instantAt(day4, 14, 0));

        // 3. Abrir segundo turno simultáneo debe fallar
        DomainException exDoubleOpen = assertThrows(DomainException.class, () -> salesService.openJornada(instantAt(day4, 15, 0)));
        assertEquals(ErrorCode.JORNADA_ALREADY_OPEN, exDoubleOpen.getErrorCode());

        // 4. Venta con cantidad <= 0 debe fallar
        DomainException exZeroQty = assertThrows(DomainException.class, () -> salesService.sell(aguila.getId(), 0));
        assertEquals(ErrorCode.INVALID_QUANTITY, exZeroQty.getErrorCode());

        DomainException exNegQty = assertThrows(DomainException.class, () -> salesService.sell(aguila.getId(), -5));
        assertEquals(ErrorCode.INVALID_QUANTITY, exNegQty.getErrorCode());

        // 5. Venta sin stock suficiente debe fallar
        inventoryService.receiveStock(aguila.getId(), 5, 4000L, instantAt(day4, 14, 30), "Stock pequeño");
        DomainException exInsStock = assertThrows(DomainException.class, () -> salesService.sell(aguila.getId(), 6));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, exInsStock.getErrorCode());

        // 6. Venta de producto archivado debe fallar
        inventoryService.archiveProduct(aguila.getId());
        DomainException exArchived = assertThrows(DomainException.class, () -> salesService.sell(aguila.getId(), 1));
        assertEquals(ErrorCode.PRODUCT_INACTIVE, exArchived.getErrorCode());

        // Desarchivar para continuar
        inventoryService.updateProduct(aguila.withActive(true));

        // 7. Anulación de venta ya anulada debe fallar
        Sale sValid = salesService.sell(aguila.getId(), 2);
        salesService.voidSale(sValid.getId());
        DomainException exDoubleVoid = assertThrows(DomainException.class, () -> salesService.voidSale(sValid.getId()));
        assertEquals(ErrorCode.SALE_ALREADY_VOIDED, exDoubleVoid.getErrorCode());

        // 8. Gasto con valor <= 0 debe fallar
        DomainException exNegExpense = assertThrows(DomainException.class, () -> expenseService.add("Test", -1000L, Instant.now(), shift.getId()));
        assertEquals(ErrorCode.INVALID_AMOUNT, exNegExpense.getErrorCode());

        // 9. Entrada de stock con cantidad <= 0 o costo negativo debe fallar
        assertThrows(DomainException.class, () -> inventoryService.receiveStock(poker.getId(), 0, 3800L, Instant.now(), ""));
        assertThrows(DomainException.class, () -> inventoryService.receiveStock(poker.getId(), 10, -500L, Instant.now(), ""));

        // 10. Conteo físico negativo debe ser rechazado
        Map<Long, Integer> negCount = new HashMap<>();
        negCount.put(aguila.getId(), -5);
        DomainException exNegCount = assertThrows(DomainException.class, () -> salesService.closeJornada(shift.getId(), instantAt(day4, 22, 0), negCount));
        assertEquals(ErrorCode.INVALID_QUANTITY, exNegCount.getErrorCode());

        // 11. Cerrar turno
        salesService.closeJornada(shift.getId(), instantAt(day4, 22, 0), Collections.emptyMap());

        // 12. Reabrir turno cerrado y operar
        Jornada reopened = salesService.reopenLastJornada();
        assertTrue(reopened.isOpen());
        salesService.sell(aguila.getId(), 1);
        salesService.closeJornada(reopened.getId(), instantAt(day4, 23, 0), Collections.emptyMap());
    }

    @Test
    @DisplayName("Día 5: Prueba de Fuego de Respaldo JSON Completo tras Múltiples Días y Múltiples Lotes")
    void testDay5_FullMultiDayBackupRoundtripIntegrity() {
        LocalDate d1 = LocalDate.of(2026, 10, 1);
        LocalDate d2 = LocalDate.of(2026, 10, 2);

        // Operaciones Día 1:
        inventoryService.receiveStock(aguila.getId(), 24, 4000L, instantAt(d1, 10, 0), "Lote 1");
        inventoryService.receiveStock(aguila.getId(), 24, 4500L, instantAt(d1, 11, 0), "Lote 2");
        Jornada j1 = salesService.openJornada(instantAt(d1, 14, 0));
        Sale sale1 = salesService.sell(aguila.getId(), 10, instantAt(d1, 15, 0)); // sale 1
        Sale sale2 = salesService.sell(aguila.getId(), 20, instantAt(d1, 17, 0)); // sale 2 (cruza del lote 1 al lote 2!)
        expenseService.add("Gasto Día 1", 12000L, instantAt(d1, 16, 0), j1.getId());
        salesService.closeJornada(j1.getId(), instantAt(d1, 22, 0), Collections.emptyMap());

        // Operaciones Día 2:
        Jornada j2 = salesService.openJornada(instantAt(d2, 14, 0));
        Sale sale3 = salesService.sell(aguila.getId(), 5, instantAt(d2, 16, 0)); // sale 3
        expenseService.add("Gasto Día 2", 8000L, instantAt(d2, 16, 0), j2.getId());
        salesService.closeJornada(j2.getId(), instantAt(d2, 22, 0), Collections.emptyMap());

        // Capturar estado previo al respaldo
        ProductMarginReport marginBefore = reportService.margins(aguila.getId());
        DailyReport daily1Before = reportService.dailyReport(d1);
        DailyReport daily2Before = reportService.dailyReport(d2);
        JornadaSummary j1Before = reportService.summary(j1.getId());
        JornadaSummary j2Before = reportService.summary(j2.getId());

        // Exportar a JSON
        String json = backupService.exportJson();
        assertNotNull(json);

        // Limpiar y restaurar
        backupService.importJson(json);

        // Verificaciones matemáticas exactas post-restauración
        ProductMarginReport marginAfter = reportService.margins(aguila.getId());
        assertEquals(marginBefore.getTotalSales(), marginAfter.getTotalSales());
        assertEquals(marginBefore.getTotalCost(), marginAfter.getTotalCost());
        assertEquals(marginBefore.getRealProfit(), marginAfter.getRealProfit());
        assertEquals(marginBefore.getUnitsSold(), marginAfter.getUnitsSold());

        DailyReport daily1After = reportService.dailyReport(d1);
        assertEquals(daily1Before.getTotalSales(), daily1After.getTotalSales());
        assertEquals(daily1Before.getTotalCost(), daily1After.getTotalCost());
        assertEquals(daily1Before.getGrossProfit(), daily1After.getGrossProfit());
        assertEquals(daily1Before.getTotalExpenses(), daily1After.getTotalExpenses());
        assertEquals(daily1Before.getNetProfit(), daily1After.getNetProfit());

        DailyReport daily2After = reportService.dailyReport(d2);
        assertEquals(daily2Before.getTotalSales(), daily2After.getTotalSales());
        assertEquals(daily2Before.getTotalCost(), daily2After.getTotalCost());
        assertEquals(daily2Before.getGrossProfit(), daily2After.getGrossProfit());
        assertEquals(daily2Before.getTotalExpenses(), daily2After.getTotalExpenses());
        assertEquals(daily2Before.getNetProfit(), daily2After.getNetProfit());

        JornadaSummary j1After = reportService.summary(j1.getId());
        assertEquals(j1Before.getTotalSales(), j1After.getTotalSales());
        assertEquals(j1Before.getTotalCost(), j1After.getTotalCost());
        assertEquals(j1Before.getGrossProfit(), j1After.getGrossProfit());

        JornadaSummary j2After = reportService.summary(j2.getId());
        assertEquals(j2Before.getTotalSales(), j2After.getTotalSales());
        assertEquals(j2Before.getTotalCost(), j2After.getTotalCost());
        assertEquals(j2Before.getGrossProfit(), j2After.getGrossProfit());

        // Verificar que las asignaciones por venta no fueron corrompidas ni duplicadas
        List<SaleLotAllocation> allocsSale1 = saleRepository.findAllocationsBySaleId(sale1.getId());
        assertEquals(1, allocsSale1.size(), "Venta 1 solo debe tener 1 asignación");
        assertEquals(10, allocsSale1.get(0).getQuantity());

        List<SaleLotAllocation> allocsSale2 = saleRepository.findAllocationsBySaleId(sale2.getId());
        assertEquals(2, allocsSale2.size(), "Venta 2 consumió de 2 lotes (14 del lote 1 y 6 del lote 2)");
        assertEquals(20, allocsSale2.stream().mapToInt(SaleLotAllocation::getQuantity).sum());
    }
}
