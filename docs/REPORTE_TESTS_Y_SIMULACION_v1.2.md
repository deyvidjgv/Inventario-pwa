# Reporte de Testing, Simulación Extrema y Auditoría v1.2.0

**Fecha:** 11/10/2026  
**Alcance:** Auditoría profunda de código, simulación multi-día (7 días de operación real continua de bar y billar pool), pruebas de estrés en persistencia Room, repositorios, DAOs, sincronización UI y ciclo de vida de Fragments.  
**Estado:** ✅ **TODOS LOS TESTS PASARON EXITOSAMENTE (0 ERRORES). NUEVOS BUGS ENCONTRADOS Y CORREGIDOS.**

---

## 1. Metodología de Pruebas y Simulación Realista

Se diseñó e implementó una nueva batería de pruebas automatizadas que simulan **7 días completos de operación continua** de un negocio real con venta de bebidas con inventario (cervezas, licores), productos sin inventario (fichas/horas de mesas de billar pool) y gastos operativos:

1. **Lunes (Día 1 - Apertura de semana):**
   - Recepción de lotes iniciales de proveedores.
   - Venta mixta de productos con control de stock y servicios por hora de pool (costo $0).
   - Gastos de turno (hielo y vasos).
   - Cierre de turno cuadrado con arqueo físico exacto.
2. **Martes (Día 2 - Rollback y Merma):**
   - Venta regular.
   - Error de digitación del cliente y anulación inmediata (`voidSale`): verificación de reversión FIFO exacta al lote de origen.
   - Conteo físico con faltante de inventario (merma por botella rota): registro del ajuste y reducción de stock en lotes activos.
3. **Miércoles (Día 3 - Inflación y Trans-Lote FIFO):**
   - Recepción de nuevo lote con costo unitario superior.
   - Venta que supera el remanente del lote antiguo y consume del lote nuevo: verificación de múltiples asignaciones con costos unitarios independientes en una sola venta.
4. **Jueves (Día 4 - Aumento de Precio de Venta):**
   - Modificación del precio de venta al público en caliente mediante `updateProduct`.
   - Verificación de que las ventas históricas preservan su precio original y las nuevas aplican el nuevo precio.
5. **Viernes (Día 5 - Fin de Semana y Turno que Cruza Medianoche):**
   - Doble jornada (turno tarde y turno noche que finaliza a las 3:00 AM del sábado).
   - Cierre con sobrante físico (+2 unidades): creación automática de lote de ajuste sobrante.
6. **Sábado (Día 6 - Re-apertura y Corrección):**
   - Re-apertura de la última jornada cerrada para asentar ventas omitidas.
7. **Domingo (Día 7 - Auditoría y Respaldo Total):**
   - Generación de reportes diarios de cada fecha.
   - Exportación completa a JSON, vaciado absoluto de la base de datos y restauración desde cero.
   - Verificación de conciliación matemática al 100% pre y post respaldo.

---

## 2. Errores y Hallazgos Detectados en la Auditoría (BUG-023 a BUG-027)

Durante el escaneo detallado del código y la simulación se encontraron los siguientes 5 problemas adicionales:

### [BUG-023] 🔴 Restaurar un respaldo dejaba la base de datos sin productos, categorías ni jornadas
- **Archivos:** `RoomProductRepository.java`, `RoomCategoryRepository.java`, `RoomJornadaRepository.java`.
- **Causa:** Al corregir BUG-007, se agregó una condición que si la entidad tenía `id != null`, llamaba a `dao.update()`. Durante la restauración de un respaldo, todas las entidades vienen con su ID, pero las tablas acaban de ser vaciadas con `deleteAll()`. Por lo tanto, `dao.update()` intentaba actualizar filas inexistentes (0 filas afectadas) y **nunca insertaba los registros**. Como consecuencia, la base de datos quedaba vacía y las tablas hijas fallaban con violación de llave foránea.
- **Corrección:** Se modificó la condición a `if (entity.getId() != null && dao.findById(entity.getId()) != null)`: si ya existe en la base de datos, se actualiza en sitio (`@Update`); si no existe (registro nuevo o restauración de respaldo), se inserta respetando el ID original (`@Insert`).
- **Prueba añadida:** `testRoomProductRepositoryInsertVsUpdateOnRestoreAndEdit` y `testRoomCategoryRepositoryInsertVsUpdateOnRestoreAndEdit` en `AppUnitIntegrationTest.java`.

### [BUG-024] 🟠 En la pantalla de Inventario no existía forma visual de ver ni desarchivar productos
- **Archivos:** `InventoryFragment.java`, `fragment_inventory.xml`.
- **Causa:** La variable `showArchived` existía en el fragmento y en el servicio, pero en el layout XML no había ningún interruptor o checkbox para cambiarla a `true`. Un usuario nunca podía ver los productos archivados para tocarlos y pulsar "Activar".
- **Corrección:** Se agregó un checkbox `MaterialCheckBox` (`R.id.check_show_archived`) en la cabecera de la lista de inventario. Al marcarlo, la lista se recarga mostrando los productos archivados con el botón "Activar".

### [BUG-025] 🟡 Los gastos en la pantalla de Gastos aparecían en orden inverso (más viejos primero)
- **Archivos:** `ExpenseDao.java`.
- **Causa:** La consulta `findByPeriod` ordenaba con `ORDER BY createdAt ASC`. En la pantalla de gastos (que consulta los últimos 30 días), los gastos más antiguos aparecían arriba y los gastos recién registrados de hoy aparecían al final de la lista, obligando al usuario a desplazarse hasta el fondo.
- **Corrección:** Se cambió el orden a `ORDER BY createdAt DESC` para que los gastos más recientes aparezcan siempre en la parte superior.

### [BUG-026] 🟡 Posible cierre inesperado por excepciones no capturadas en `SalesFragment`
- **Archivos:** `SalesFragment.java`.
- **Causa:** Los métodos `executeSale()`, `voidLastSale()` y `showSessionSalesDialog()` capturaban exclusivamente `DomainException`. Cualquier otra excepción en segundo plano (como errores de SQLite o del sistema) no era capturada y causaba el cierre abrupto de la aplicación.
- **Corrección:** Se envolvieron todas las llamadas en bloques `try/catch (Exception e)` garantizando notificación al usuario vía `Toast` sin crashear.

### [BUG-027] 🟢 `MaterialButton.setBackgroundColor` rompía el diseño visual en productos agotados
- **Archivos:** `SalesFragment.java`.
- **Causa:** En Android Material Components, llamar directamente a `setBackgroundColor()` sobre un `MaterialButton` elimina el drawable vectorial de bordes redondeados, dejando esquinas cuadradas y perdiendo el estilo del tema.
- **Corrección:** Se reemplazó por `btn.setBackgroundTintList(ColorStateList.valueOf(...))` preservando los bordes redondeados y la estética original.

---

## 3. Resultados de la Verificación Automatizada

- **`:domain:test`**: **21 de 21 tests PASARON** (100% de éxito).
  - Casos de negocio obligatorios (Casos A a H)
  - Simulación multi-día realista (Días 1 a 5 y Semana Completa de 7 días)
  - Pruebas de borde, validación de stock, mermas, anulación y respaldo
- **`:app:testDebugUnitTest`**: **100% PASARON** (sin fallos).
  - Pruebas de formato de moneda COP
  - Convertidores de tipos Room (Instant, LocalDate)
  - Mapeo bidireccional de entidades de dominio
  - Pruebas de inserción vs actualización en repositorios Room
  - Pruebas de Gson TypeAdapter seguro ante nulos
  - Cálculos de zona horaria `America/Bogota`
- **`./gradlew check`**: **BUILD SUCCESSFUL** (Lint + Tests sin errores).
- **`./gradlew assembleDebug`**: **BUILD SUCCESSFUL**, nuevo binario generado en `InventarioPool.apk` (6.2 MB).
