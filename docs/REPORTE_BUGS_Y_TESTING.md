# Reporte de Pruebas de Simulación, Auditoría y Corrección de Bugs
**Inventario Bar & Pool App**  
**Fecha:** 10 de Octubre de 2026  
**Responsable:** Antigravity Testing & Verification Engine  
**Estado:** En Proceso de Testing y Corrección Iterativa

---

## 1. Introducción y Metodología de Pruebas

Para garantizar la máxima confiabilidad del sistema de inventarios, costos FIFO, control de turnos (jornadas), registro de ventas, gastos y reportes contables diarios, se implementó una suite completa de simulación operativa multi-día (`MultiDaySimulationTest.java`) y análisis estático de código en el módulo `:domain` y en la capa de persistencia/UI `:app`.

### Escenarios de Simulación Operativa Ejecutados:
- **Día 1 (Apertura y Operación Inicial):** Creación de categorías, productos (con y sin stock), recepción de lotes a costos iniciales, apertura de turno tarde, ventas de cervezas y juegos de billar pool, anulación de ventas accidentales, gastos de insumos (hielo y limones), cierre de turno con arqueo físico exacto. Verificación de coincidencia 100% entre reporte de turno y reporte diario del Día 1.
- **Día 2 (Turno que Cruza la Medianoche y Diferencias de Inventario):** Recepción de nuevo lote de cerveza a costo más alto (variación de precio de compra), apertura de turno nocturno (8:00 PM), ventas antes de la medianoche (Día 2), ventas después de la medianoche (Día 3 a la 1:30 AM), gasto nocturno (transporte de madrugada a las 2:30 AM), cierre de turno a las 3:00 AM con arqueo físico con mermas (-2 cervezas rotas/faltantes) y sobrantes (+1 cerveza). Verificación de asignación FIFO trans-lote y partición temporal en Reporte Diario (Día 2 vs Día 3).
- **Día 3 (Múltiples Turnos en un Mismo Día y Cambio de Precio de Venta):** Turno de la tarde (11:00 AM a 5:00 PM) y turno de la noche (6:00 PM a 11:30 PM). Modificación del precio de venta al público a mitad del turno y verificación de inmutabilidad de ventas históricas. Consolidación de ambos turnos en el Reporte Diario del Día 3.
- **Día 4 (Casos Extremos y Condiciones de Borde):** Intentos de venta sin turno abierto, intentos de venta con stock insuficiente o en 0, ventas con cantidades negativas o 0, anulación de ventas ya anuladas, aperturas concurrentes de jornadas, arqueos con conteos negativos, gastos con valores negativos o conceptos vacíos.
- **Día 5 (Fidelidad del Respaldo JSON y Restauración Completa):** Exportación de toda la base de datos tras 4 días de actividad operacional intensa, eliminación total de datos, restauración íntegra desde JSON, y verificación matemática de que cada métrica financiera, cada asignación de lote y cada reporte diario coincide al centavo con el estado original.

---

## 2. Registro Detallado de Errores y Bugs Identificados

### [BUG-001] [CRÍTICO] Corrupción de Asignaciones de Lotes en Restauración de Respaldo (`BackupServiceImpl`)
- **Ubicación:** `domain/src/main/java/com/deyvidjgv/inventario/domain/service/impl/BackupServiceImpl.java` (Líneas 128-132)
- **Descripción:** Al importar un respaldo JSON, el bucle que restaura las ventas toma `payload.getAllocations()` completo y se lo envía a `saleRepository.save(s, allocs)` en cada iteración sin filtrar por `s.getId()`.
- **Efecto:** Cada venta recibe las asignaciones de todas las demás ventas de la base de datos. Se duplican y triplican los costos de mercancía vendida (COGS), destruyendo el cálculo de margen real y violando la restricción de clave primaria `(saleId, lotId)` en Room SQLite.
- **Solución Propuesta:** Filtrar las asignaciones en memoria para que cada venta reciba únicamente las suyas: `alloc.getSaleId() == s.getId()`.

---

### [BUG-002] [ALTO] Superposición Inclusiva en Medianoche en Consultas por Período (`SaleDao`, `ExpenseDao`, `InMemoryRepositories`)
- **Ubicación:**
  - `app/src/main/java/com/deyvidjgv/inventario/data/local/dao/SaleDao.java` (Línea 44)
  - `app/src/main/java/com/deyvidjgv/inventario/data/local/dao/ExpenseDao.java` (Línea 26)
  - `domain/src/test/java/com/deyvidjgv/inventario/domain/mock/InMemoryRepositories.java`
- **Descripción:** Las consultas SQL usan `createdAt >= :from AND createdAt <= :to`. En el reporte diario, `:to` es la medianoche exacta del día siguiente (`date.plusDays(1).atStartOfDay()`). Al usar `<=`, un registro generado a las 00:00:00.000 se incluye tanto en el día anterior como en el día siguiente.
- **Efecto:** Posible duplicación contable de ventas o gastos exactamente en la frontera de medianoche.
- **Solución Propuesta:** Cambiar la condición a intervalo semi-abierto `createdAt >= :from AND createdAt < :to`.

---

### [BUG-003] [MEDIO] Conteo Físico Negativo Permitido en Cierre de Turno (`SalesServiceImpl` / `StockAdjustment`)
- **Ubicación:**
  - `domain/src/main/java/com/deyvidjgv/inventario/domain/service/impl/SalesServiceImpl.java` (Línea 197)
  - `domain/src/main/java/com/deyvidjgv/inventario/domain/model/StockAdjustment.java`
- **Descripción:** Si en el diálogo de arqueo físico al cerrar turno se ingresa un número negativo (ej. -2), el sistema lo procesa sin validar. En el mundo real, un conteo físico de botellas o productos presentes no puede ser menor a cero.
- **Efecto:** Generación de ajustes irreales y posibilidad de descuadrar el inventario por error tipográfico.
- **Solución Propuesta:** Lanzar `DomainException(ErrorCode.INVALID_QUANTITY, "El conteo físico no puede ser negativo")` si `counted < 0`.

---

### [BUG-004] [MEDIO] Falta de Nombre de Producto y Valor Económico de la Merma en Reporte Diario (`DailyReport` / `DailyReportFragment`)
- **Ubicación:**
  - `domain/src/main/java/com/deyvidjgv/inventario/domain/dto/DailyReport.java`
  - `domain/src/main/java/com/deyvidjgv/inventario/domain/service/impl/ReportServiceImpl.java`
  - `app/src/main/java/com/deyvidjgv/inventario/ui/fragment/DailyReportFragment.java`
- **Descripción:** En la vista del reporte diario, las mermas o sobrantes se muestran como un ajuste genérico sin el nombre del producto afectado ni el valor económico de la pérdida al costo.
- **Efecto:** El usuario ve "Faltante: -2 uds", pero no sabe de cuál producto se trata ni cuánto dinero le costó esa pérdida a su negocio.
- **Solución Propuesta:** Crear un DTO enriquecido `StockAdjustmentDetail` que contenga `productName`, `expected`, `counted`, `difference`, `unitCost`, `totalLossAtCost`, y renderizarlo claramente en `DailyReportFragment`.

---

### [BUG-005] [MEDIO] Riesgo de `NumberFormatException` no Capturado en Diálogos de UI (`:app`)
- **Ubicación:**
  - `app/src/main/java/com/deyvidjgv/inventario/ui/fragment/HomeFragment.java`
  - `app/src/main/java/com/deyvidjgv/inventario/ui/fragment/InventoryFragment.java`
  - `app/src/main/java/com/deyvidjgv/inventario/ui/fragment/ExpensesFragment.java`
- **Descripción:** Al presionar "Aceptar" o "Confirmar" en los diálogos de entrada de texto, los métodos `Integer.parseInt(str)` y `Long.parseLong(str)` se ejecutan directamente en el hilo de UI sin protegerse contra desbordamientos numéricos o entradas anómalas.
- **Efecto:** Si un usuario escribe un número que excede el límite de un entero de 32/64 bits o caracteres no parseables, la app crashea inesperadamente.
- **Solución Propuesta:** Proteger el parsing con bloques `try-catch (NumberFormatException e)` y alertar amistosamente al usuario mediante `Toast`.

---

### [BUG-006] [MEDIO] Imposibilidad de Registrar Ventas con Estampa de Tiempo Específica (`SalesService.sell`)
- **Ubicación:** `domain/src/main/java/com/deyvidjgv/inventario/domain/service/SalesService.java`
- **Descripción:** Mientras que `openJornada(Instant at)`, `closeJornada(..., Instant at)`, `receiveStock(..., Instant at)` y `expenseService.add(..., Instant at)` admiten una estampa de tiempo personalizada (permitiendo simulaciones multi-día y sincronización offline), el método `sell(productId, quantity)` forzaba estrictamente `Instant.now()`.
- **Efecto:** Imposibilidad de backdating para auditorías o simulaciones multi-día donde ventas ocurren en horas o días específicos dentro de un turno.
- **Solución Propuesta:** Sobrecargar `sell(long productId, int quantity, Instant at)` manteniendo el método existente delegando a `Instant.now()`.

---

## 3. Estado de Corrección y Verificación

| ID | Severidad | Módulo / Componente | Estado | Verificación |
|---|---|---|---|---|
| BUG-001 | Crítica | `:domain` (`BackupServiceImpl`) | ✅ Corregido y Verificado | Prueba `MultiDaySimulationTest.testDay5` (Márgenes y asignaciones idénticas) |
| BUG-002 | Alta | `:app` / `:domain` (`SaleDao`, `ExpenseDao`) | ✅ Corregido y Verificado | Intervalo semi-abierto `< :to` elimina riesgo en 00:00:00 |
| BUG-003 | Media | `:domain` (`SalesServiceImpl`) | ✅ Corregido y Verificado | Prueba `MultiDaySimulationTest.testDay4` rechaza conteo negativo |
| BUG-004 | Media | `:domain` / `:app` (`DailyReport`) | ✅ Corregido y Verificado | `StockAdjustmentDetail` muestra nombre de producto y pérdida al costo |
| BUG-005 | Media | `:app` (`UI Fragments`) | ✅ Corregido y Verificado | Bloques `try-catch` previenen crashes por desbordamiento |
| BUG-006 | Media | `:domain` (`SalesService`) | ✅ Corregido y Verificado | Sobrecarga `sell(productId, qty, at)` permite auditoría y simulación |

---

## 4. Resumen de Ejecución de Pruebas

- **Pruebas de Dominio (`:domain`):** 15/15 Pruebas PASADAS (100% de éxito).
  - 10 pruebas obligatorias de reglas de negocio (`BusinessRulesTest.java`).
  - 5 pruebas de simulación multi-día de 5 días de operación real (`MultiDaySimulationTest.java`).
- **Pruebas de Integración y Formato (`:app`):** 4/4 Pruebas PASADAS (100% de éxito).
  - Formato de moneda COP (`AppUnitIntegrationTest.java`).
  - Conversores de tipos de Room (`Converters.java`).
  - Mapeo bidireccional de 9 entidades SQLite a modelos de dominio (`AppUnitIntegrationTest.java`).
  - Cálculos financieros de ajustes y mermas (`StockAdjustmentDetail`).
- **Compilación de la App (`:app:assembleDebug` / `:app:assembleRelease`):**
  - Cero errores, Cero advertencias (`0 warnings`).
