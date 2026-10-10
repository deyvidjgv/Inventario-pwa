package com.deyvidjgv.inventario;

import com.deyvidjgv.inventario.data.local.converter.Converters;
import com.deyvidjgv.inventario.data.local.entity.*;
import com.deyvidjgv.inventario.domain.dto.StockAdjustmentDetail;
import com.deyvidjgv.inventario.domain.model.*;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class AppUnitIntegrationTest {

    @Test
    public void testCurrencyFormatterCOP() {
        assertEquals("$0", CurrencyFormatter.formatCOP(0));
        assertEquals("$5.000", CurrencyFormatter.formatCOP(5000));
        assertEquals("$180.000", CurrencyFormatter.formatCOP(180000));
        assertEquals("$1.500.000", CurrencyFormatter.formatCOP(1500000));
        assertEquals("-$20.000", CurrencyFormatter.formatCOP(-20000));
    }

    @Test
    public void testRoomConverters() {
        Instant now = Instant.now();
        Long ts = Converters.instantToTimestamp(now);
        assertNotNull(ts);
        assertEquals(now.toEpochMilli(), ts.longValue());

        Instant restored = Converters.fromTimestamp(ts);
        assertEquals(now.toEpochMilli(), restored.toEpochMilli());

        assertNull(Converters.instantToTimestamp(null));
        assertNull(Converters.fromTimestamp(null));

        LocalDate date = LocalDate.of(2026, 10, 10);
        String dateStr = Converters.localDateToString(date);
        assertEquals("2026-10-10", dateStr);

        LocalDate restoredDate = Converters.fromDateString(dateStr);
        assertEquals(date, restoredDate);

        assertNull(Converters.localDateToString(null));
        assertNull(Converters.fromDateString(null));
    }

    @Test
    public void testEntityDomainBidirectionalMapping() {
        // 1. Category
        Category category = new Category(10L, "Cervezas");
        CategoryEntity catEntity = CategoryEntity.fromDomain(category);
        assertEquals(Long.valueOf(10L), catEntity.id);
        assertEquals("Cervezas", catEntity.name);
        Category catRestored = catEntity.toDomain();
        assertEquals(category.getId(), catRestored.getId());
        assertEquals(category.getName(), catRestored.getName());

        // 2. Product
        Product product = new Product(1L, "Cerveza Águila", 10L, 6000L, true, true);
        ProductEntity prodEntity = ProductEntity.fromDomain(product);
        assertEquals(Long.valueOf(1L), prodEntity.id);
        assertEquals("Cerveza Águila", prodEntity.name);
        assertEquals(10L, prodEntity.categoryId);
        assertEquals(6000L, prodEntity.salePrice);
        assertTrue(prodEntity.tracksStock);
        assertTrue(prodEntity.active);
        Product prodRestored = prodEntity.toDomain();
        assertEquals(product, prodRestored);

        // 3. StockLot
        Instant now = Instant.now();
        StockLot lot = new StockLot(5L, 1L, now, 50, 42, 4000L, "Lote 1");
        StockLotEntity lotEntity = StockLotEntity.fromDomain(lot);
        assertEquals(Long.valueOf(5L), lotEntity.id);
        assertEquals(1L, lotEntity.productId);
        assertEquals(50, lotEntity.quantityIn);
        assertEquals(42, lotEntity.quantityRemaining);
        assertEquals(4000L, lotEntity.unitCost);
        assertEquals("Lote 1", lotEntity.note);
        assertEquals(lot, lotEntity.toDomain());

        // 4. Jornada
        Instant closedAt = now.plusSeconds(3600);
        Jornada jornada = new Jornada(7L, now, closedAt);
        JornadaEntity jEntity = JornadaEntity.fromDomain(jornada);
        assertEquals(Long.valueOf(7L), jEntity.id);
        assertEquals(now, jEntity.openedAt);
        assertEquals(closedAt, jEntity.closedAt);
        assertEquals(jornada, jEntity.toDomain());

        // 5. Sale
        Sale sale = new Sale(100L, 7L, 1L, 10, 6000L, now, false);
        SaleEntity saleEntity = SaleEntity.fromDomain(sale);
        assertEquals(Long.valueOf(100L), saleEntity.id);
        assertEquals(7L, saleEntity.jornadaId);
        assertEquals(1L, saleEntity.productId);
        assertEquals(10, saleEntity.quantity);
        assertEquals(6000L, saleEntity.unitPrice);
        assertEquals(sale, saleEntity.toDomain());

        // 6. SaleLotAllocation
        SaleLotAllocation alloc = new SaleLotAllocation(100L, 5L, 10, 4000L);
        SaleLotAllocationEntity allocEntity = SaleLotAllocationEntity.fromDomain(alloc);
        assertEquals(100L, allocEntity.saleId);
        assertEquals(5L, allocEntity.lotId);
        assertEquals(10, allocEntity.quantity);
        assertEquals(4000L, allocEntity.unitCost);
        assertEquals(alloc, allocEntity.toDomain());

        // 7. StockAdjustment
        StockAdjustment adj = new StockAdjustment(3L, 1L, 7L, 44, 42, -2, now);
        StockAdjustmentEntity adjEntity = StockAdjustmentEntity.fromDomain(adj);
        assertEquals(Long.valueOf(3L), adjEntity.id);
        assertEquals(1L, adjEntity.productId);
        assertEquals(7L, adjEntity.jornadaId);
        assertEquals(44, adjEntity.expected);
        assertEquals(42, adjEntity.counted);
        assertEquals(-2, adjEntity.difference);
        assertEquals(adj, adjEntity.toDomain());

        // 8. Expense
        Expense exp = new Expense(9L, "Hielo", 15000L, now, 7L);
        ExpenseEntity expEntity = ExpenseEntity.fromDomain(exp);
        assertEquals(Long.valueOf(9L), expEntity.id);
        assertEquals("Hielo", expEntity.concept);
        assertEquals(15000L, expEntity.amount);
        assertEquals(Long.valueOf(7L), expEntity.jornadaId);
        assertEquals(exp, expEntity.toDomain());

        // 9. AuditLog
        AuditLog audit = new AuditLog(20L, "Sale", 100L, "VOID", "{}", "{\"voided\":true}", now);
        AuditLogEntity auditEntity = AuditLogEntity.fromDomain(audit);
        assertEquals(Long.valueOf(20L), auditEntity.id);
        assertEquals("Sale", auditEntity.entity);
        assertEquals(100L, auditEntity.entityId);
        assertEquals("VOID", auditEntity.action);
        assertEquals(audit, auditEntity.toDomain());
    }

    @Test
    public void testStockAdjustmentDetailCalculations() {
        StockAdjustmentDetail detail = new StockAdjustmentDetail(
                1L, 10L, "Cerveza Poker", 3L, 20, 18, -2, 3800L, -7600L, Instant.now()
        );
        assertEquals("Cerveza Poker", detail.getProductName());
        assertEquals(-2, detail.getDifference());
        assertEquals(3800L, detail.getUnitCost());
        assertEquals(-7600L, detail.getTotalLossOrGainAtCost());
    }

    @Test
    public void testGsonInstantTypeAdapterWithNull_BUG021() {
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                .registerTypeAdapter(Instant.class, new com.google.gson.TypeAdapter<Instant>() {
                    @Override
                    public void write(com.google.gson.stream.JsonWriter out, Instant value) throws java.io.IOException {
                        if (value == null) {
                            out.nullValue();
                        } else {
                            out.value(value.toString());
                        }
                    }

                    @Override
                    public Instant read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
                        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                            in.nextNull();
                            return null;
                        }
                        return Instant.parse(in.nextString());
                    }
                })
                .create();

        // 1. Serialization with null
        Jornada jornada = new Jornada(1L, Instant.parse("2026-10-10T12:00:00Z"), null);
        String json = gson.toJson(jornada);
        assertNotNull(json);

        // 2. Deserialization with null
        Jornada deserialized = gson.fromJson(json, Jornada.class);
        assertEquals(Long.valueOf(1L), deserialized.getId());
        assertEquals(Instant.parse("2026-10-10T12:00:00Z"), deserialized.getOpenedAt());
        assertNull(deserialized.getClosedAt());
    }

    @Test
    public void testProductAndCategoryPreservesIdForUpdate_BUG007() {
        // En SQLite Room, si se usa INSERT OR REPLACE con FK RESTRICT, se produce error.
        // La entidad debe preservar su ID para que el repositorio ejecute DAO.update() en vez de insert().
        Product prod = new Product(42L, "Águila Light", 1L, 5000L, true, true);
        ProductEntity entity = ProductEntity.fromDomain(prod);
        assertNotNull(entity.id);
        assertEquals(Long.valueOf(42L), entity.id);

        Product updatedDomain = new Product(42L, "Águila Light 330ml", 1L, 5500L, true, true);
        ProductEntity updatedEntity = ProductEntity.fromDomain(updatedDomain);
        assertEquals(entity.id, updatedEntity.id);
        assertEquals("Águila Light 330ml", updatedEntity.name);
        assertEquals(5500L, updatedEntity.salePrice);
    }

    @Test
    public void testDailyReportBogotaTimezoneBounds_BUG020() {
        java.time.ZoneId bogotaZone = java.time.ZoneId.of("America/Bogota");
        LocalDate date = LocalDate.of(2026, 10, 10);
        Instant startOfDay = date.atStartOfDay(bogotaZone).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(bogotaZone).toInstant();

        // En UTC, America/Bogota (UTC-5) a medianoche es a las 05:00 UTC del mismo día
        assertEquals(Instant.parse("2026-10-10T05:00:00Z"), startOfDay);
        assertEquals(Instant.parse("2026-10-11T05:00:00Z"), endOfDay);
        assertTrue(endOfDay.isAfter(startOfDay));
    }
}
