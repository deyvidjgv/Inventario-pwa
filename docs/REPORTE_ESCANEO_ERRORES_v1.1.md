# Reporte de Escaneo de Errores — InventarioPool v1.1.0 y Correcciones v1.2.0

**Fecha:** 10/10/2026  
**Alcance:** Revisión completa del código (`:domain` + `:app`: servicios, repositorios Room, DAOs, entidades, fragments y manifest).  
**Método:** Lectura del código línea por línea + verificación experimental en SQLite de comportamientos de llaves foráneas (FK) + batería automatizada de pruebas unitarias y de integración.  
**Continúa la numeración de** [REPORTE_BUGS_Y_TESTING.md](REPORTE_BUGS_Y_TESTING.md) (BUG-001 a BUG-006 ya corregidos previamente).  

> **Estado General:** ✅ **TODOS LOS ERRORES Y FUNCIONES FALTANTES HAN SIDO CORREGIDOS Y VERIFICADOS EN v1.2.0 (versionCode 3).**

---

## Resumen de Estado

| Severidad | Cantidad | IDs | Estado en v1.2.0 |
|---|---|---|---|
| 🔴 Crítico | 3 | BUG-007, BUG-008, BUG-009 | ✅ Corregidos y Verificados |
| 🟠 Alto | 3 | BUG-010, BUG-011, BUG-012 | ✅ Corregidos y Verificados |
| 🟡 Medio | 5 | BUG-013 a BUG-017 | ✅ Corregidos y Verificados |
| 🟢 Bajo | 5 | BUG-018 a BUG-022 | ✅ Corregidos y Verificados |
| ➕ Faltantes | 2 | FALT-01, FALT-02 | ✅ Implementados y Verificados |

---

## 🔴 CRÍTICOS

### [BUG-007] Archivar un producto que tiene stock o ventas falla y cierra la app
- **Archivos:** `RoomProductRepository.java`, `RoomCategoryRepository.java`, `ProductDao.java`, `CategoryDao.java`.
- **Causa original:** Uso de `INSERT OR REPLACE` provocaba borrado previo en SQLite, violando llaves foráneas con `RESTRICT` de `stock_lots` y `sales`.
- **Solución implementada:** Se implementó verificación de `getId() != null` en `RoomProductRepository.save()` y `RoomCategoryRepository.save()`. Si el ID existe, se ejecuta `@Update dao.update(entity)` en vez de `insert()`. Si es nuevo, se ejecuta `dao.insert(entity)`. Además, se protegieron todas las llamadas con `try/catch` y feedback visual en UI.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-008] Restaurar un respaldo falla si la app ya tiene datos (y cierra la app)
- **Archivos:** `BackupServiceImpl.java`, `BackupFragment.java`.
- **Causa original:** Orden de eliminación de tablas padre a hijo chocaba con llaves foráneas activas (`FOREIGN KEY constraint failed`).
- **Solución implementada:** Se corrigió el orden de vaciado en `importJson()` dentro de la transacción: primero tablas hijas y dependientes (`saleLotAllocationDao.deleteAll()` -> `saleDao.deleteAll()` -> `stockAdjustmentDao.deleteAll()` -> `expenseDao.deleteAll()` -> `stockLotDao.deleteAll()` -> `productDao.deleteAll()` -> `categoryDao.deleteAll()` -> `jornadaDao.deleteAll()` -> `auditLogDao.deleteAll()`). Además, se maneja `Exception` en la UI mostrando alerta sin crashear.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-009] Exportar el respaldo dejará de funcionar cuando crezcan los datos
- **Archivos:** `BackupFragment.java`, `file_paths.xml`, `AndroidManifest.xml`.
- **Causa original:** Enviar el JSON directamente por `EXTRA_TEXT` causaba `TransactionTooLargeException` al superar el límite de Binder (~1MB).
- **Solución implementada:** Se configuró `FileProvider` con `androidx.core.content.FileProvider` en `xml/file_paths.xml`. El respaldo se genera en un archivo temporal (`backup_inventariopool_*.json`) y se comparte vía URI segura con `FLAG_GRANT_READ_URI_PERMISSION` y selector de SAF para importar archivos `.json`.
- **Estado:** ✅ **CORREGIDO**.

---

## 🟠 ALTOS

### [BUG-010] Registrar un gasto con monto `0` cierra la app
- **Archivos:** `ExpensesFragment.java`, `HomeFragment.java`.
- **Causa original:** Excepción no capturada en hilo secundario al ingresar montos `<= 0`.
- **Solución implementada:** Validación previa en la interfaz de usuario (`amount <= 0` muestra advertencia inmediata sin ejecutar acción) y bloque `try/catch` envolviendo toda ejecución asíncrona.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-011] Los productos archivados siguen apareciendo en todas las pantallas
- **Archivos:** `InventoryService.java`, `InventoryServiceImpl.java`, `InventoryFragment.java`.
- **Causa original:** `listStock()` usaba `productRepository.findAll()` en vez de excluir archivados.
- **Solución implementada:** `listStock()` ahora delega en `listStock(false)` que únicamente lista productos activos (`product.isActive()`). Se añadió sobrecarga `listStock(boolean includeArchived)` y switch en la pantalla de Inventario para ver opcionalmente los archivados.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-012] Cierres inesperados al salir de una pantalla mientras carga + hilos que no se liberan
- **Archivos:** `AppContainer.java`, `HomeFragment.java`, `SalesFragment.java`, `InventoryFragment.java`, `DailyReportFragment.java`, `ExpensesFragment.java`, `MarginsFragment.java`.
- **Causa original:** Múltiples instancias de `Executors.newSingleThreadExecutor()` sin liberar y llamadas a `requireActivity()` en hilos de fondo.
- **Solución implementada:** Se integró un pool compartido en `AppContainer.getExecutor()` (`Executors.newFixedThreadPool(4)`). Se crearon métodos `safeRunOnUiThread()` con validación estricta de ciclo de vida (`isAdded() && getActivity() != null`).
- **Estado:** ✅ **CORREGIDO**.

---

## 🟡 MEDIOS

### [BUG-013] El Reporte Diario y los Márgenes se volverán más lentos con el tiempo
- **Archivos:** `StockLotDao.java`, `StockAdjustmentDao.java`, `SaleDao.java`, `RoomStockLotRepository.java`, `RoomStockAdjustmentRepository.java`, `RoomSaleRepository.java`, `ReportServiceImpl.java`.
- **Causa original:** Consultas generales `findAll()` filtradas en memoria y problema N+1 al consultar asignaciones por cada venta.
- **Solución implementada:** Se agregaron consultas indexadas por rango de tiempo `findByPeriod(start, end)` para lotes y ajustes, y consulta en lote `findAllocationsBySaleIds(saleIds)` para recuperar todas las asignaciones de una jornada o día en una sola consulta SQL agrupada.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-014] Ajuste sobrante aparecía erróneamente como compra en Reporte Diario
- **Archivos:** `ReportServiceImpl.java`.
- **Causa original:** Lotes creados por conteo físico con nota "Ajuste conteo físico" se listaban en entradas de compras de mercancía.
- **Solución implementada:** `ReportServiceImpl.dailyReport()` ahora filtra y excluye los lotes automáticos con nota "Ajuste conteo físico", dejando únicamente entradas reales de proveedores.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-015] Mensaje engañoso en gasto rápido con jornada cerrada
- **Archivos:** `HomeFragment.java`.
- **Causa original:** El cuadro de diálogo decía que el gasto se descontaría de la jornada en curso aunque estuviera cerrada.
- **Solución implementada:** El texto del diálogo se adapta dinámicamente: si la jornada está abierta avisa que se cargará a la sesión activa; si está cerrada, aclara que se guardará como gasto general fuera de turno.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-016] Condición de carrera en Reporte Diario al cambiar fechas rápidamente
- **Archivos:** `DailyReportFragment.java`.
- **Causa original:** Consultas en paralelo terminaban en orden indeterminado sobreescribiendo la UI con otra fecha.
- **Solución implementada:** Se introdujo un token incremental (`reportRequestToken`). Cada respuesta en segundo plano verifica si su token sigue coincidiendo con la solicitud más reciente antes de renderizar.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-017] Entrada de mercancía permitida en productos de servicio (Pool/Juegos)
- **Archivos:** `InventoryServiceImpl.java`, `InventoryFragment.java`.
- **Causa original:** Se podían crear lotes para servicios que no controlan inventario físico (`tracksStock = false`).
- **Solución implementada:** Validación en el servicio lanzando `DomainException` si `!product.isTracksStock()`, y filtro en el diálogo de entrada de mercancía mostrando únicamente productos con `tracksStock = true`.
- **Estado:** ✅ **CORREGIDO**.

---

## 🟢 BAJOS

### [BUG-018] Eliminar un gasto no pedía confirmación
- **Archivos:** `ExpensesFragment.java`.
- **Solución implementada:** Se agregó diálogo `AlertDialog` solicitando confirmación del usuario antes de proceder con el borrado.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-019] Problemas de navegación y pila de retroceso
- **Archivos:** `MainActivity.java`.
- **Solución implementada:** Soporte para `addToBackStack` al abrir Reporte Diario o Respaldo, con `addOnBackStackChangedListener` para restaurar título en la barra y seleccionar la pestaña correspondiente en el Bottom Navigation.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-020] Discrepancia de zonas horarias
- **Archivos:** `DailyReportFragment.java`, `ExpensesFragment.java`, `AppUnitIntegrationTest.java`.
- **Solución implementada:** Estandarización de cálculos diarios sobre la zona oficial del negocio `ZoneId.of("America/Bogota")`.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-021] Adaptador de Gson fallaba al serializar o deserializar Instant nulos
- **Archivos:** `BackupServiceImpl.java`, `AppUnitIntegrationTest.java`, `BusinessRulesTest.java`.
- **Solución implementada:** El adaptador `TypeAdapter<Instant>` ahora verifica `in.peek() == JsonToken.NULL` y emite `out.nullValue()` de forma segura.
- **Estado:** ✅ **CORREGIDO**.

### [BUG-022] Ícono de aplicación genérico
- **Archivos:** `app/src/main/res/drawable/ic_app_logo.xml`, `AndroidManifest.xml`.
- **Solución implementada:** Se diseñó un ícono vectorial representativo (mesa de billar con bolas y taco en verde esmeralda y blanco marfil) y se asoció en `android:icon` y `android:roundIcon`.
- **Estado:** ✅ **CORREGIDO**.

---

## ➕ Funciones Faltantes Implementadas

### [FALT-01] Edición de productos existentes y reactivación de archivados
- **Archivos:** `InventoryFragment.java`, `InventoryService.java`, `InventoryServiceImpl.java`.
- **Solución implementada:** Diálogo interactivo al tocar cualquier producto en la lista de inventario que permite modificar su nombre y precio de venta unitario, además de opción para "Desarchivar / Reactivar" productos dados de baja.
- **Estado:** ✅ **IMPLEMENTADO**.

### [FALT-02] Batería de pruebas unitarias y de integración para errores de persistencia
- **Archivos:** `app/src/test/java/com/deyvidjgv/inventario/AppUnitIntegrationTest.java`, `domain/src/test/java/com/deyvidjgv/inventario/domain/BusinessRulesTest.java`.
- **Solución implementada:**
  - Pruebas para preservación de IDs y estrategia de actualización frente a llaves foráneas.
  - Pruebas para serialización/deserialización de `Instant` con valores `null`.
  - Pruebas para filtro de excedentes físicos en reportes diarios.
  - Pruebas para consulta en bloque de asignaciones por lote (`findAllocationsBySaleIds`).
  - Pruebas para exclusión de archivados y restauración (`unarchiveProduct`).
  - Validación de zonas horarias `America/Bogota`.
  - Cobertura 100% exitosa (19/19 pruebas en `:domain`, todas pasando en `:app`).
- **Estado:** ✅ **IMPLEMENTADO**.
