# Documentación de Implementación Técnica y Trabajo Realizado
## Proyecto: Inventario Pool (Android Native APK)

---

## 1. Resumen de la Entrega

Se ha implementado la base completa del sistema conforme a la arquitectura limpia, modular y escalable definida en [`docs/ARQUITECTURA_Y_PLAN_ESCALABLE.md`](ARQUITECTURA_Y_PLAN_ESCALABLE.md) y [`docs/PLAN_TECNICO.md`](PLAN_TECNICO.md).

El proyecto está estructurado como una aplicación **Android nativa en Java 17**, con separación estricta entre el núcleo de lógica financiera y la infraestructura de Android / Room, asegurando que el código sea testeable, robusto y preparado para expansiones futuras.

---

## 2. Componentes Construidos

### A. Configuración Multi-Módulo y Scaffolding (Fase 0)
* **Entorno y Herramientas:**
  * Configurado con **JDK 17 (Temurin)** y **Gradle 8.7**.
  * Generado el ejecutable **Gradle Wrapper** (`gradlew`, `gradle/wrapper/gradle-wrapper.jar`, `gradle-wrapper.properties`).
* **Configuración Gradle:**
  * `settings.gradle`: Configuración de repositorios Maven Central / Google y declaración de módulos `:domain` y `:app`.
  * `build.gradle` (raíz): Configuración de plugins Android Application y Library.
  * `gradle.properties`: Optimizaciones de JVM y habilitación de AndroidX.
  * `.gitignore`: Exclusión de carpetas de compilación (`build/`, `.gradle/`), llaves criptográficas (`*.jks`, `*.keystore`), credenciales y archivos locales.

---

### B. Módulo `:domain` (Java 17 Puro — Sin dependencias de Android)
El núcleo de negocio no tiene dependencias de Android ni de Room, garantizando su portabilidad y escalabilidad.

#### 1. Modelos de Dominio Inmutables (`com.deyvidjgv.inventario.domain.model`):
* `Category`: Identificador y nombre.
* `Product`: Nombre, categoría, precio de venta en pesos enteros (`long`), flag `tracksStock` e inmutabilidad mediante archivado (`active = false`).
* `StockLot`: Registro de entradas por lote con cantidad inicial, cantidad restante, **precio de compra unitario obligatorio** (`unitCost`), timestamp UTC y métodos inmutables `deduct()` y `restore()`.
* `Jornada`: Apertura y cierre manual con control de estados (`isOpen()`, `close()`, `reopen()`).
* `Sale`: Venta atómica con congelamiento del `unitPrice` del momento, cantidad, timestamp y flag `voided`.
* `SaleLotAllocation`: Asignación unitaria de costo por lote para trazabilidad FIFO exacta.
* `StockAdjustment`: Registro de conteos físicos (esperado, contado, diferencia).
* `Expense`: Registro libre de gastos con concepto, monto en pesos y vinculación opcional a jornada.
* `AuditLog`: Registro de auditoría con `beforeJson` y `afterJson` para trazabilidad de anulaciones.

#### 2. Objetos de Transferencia y Reportes (`com.deyvidjgv.inventario.domain.dto`):
* `ProductStock`: Stock consolidado y desglose de lotes activos.
* `LotMargin`: Cálculo automático de margen sobre venta (%) y margen sobre costo (%) redondeado a 1 decimal.
* `ProductMarginReport`: Reporte por producto con ganancia real acumulada, ganancia simulada con costo del lote más reciente y el diferencial financiero.
* `JornadaSummary`: Balance de noche (ventas, costos, ganancia bruta, gastos libres, ganancia neta y ajustes de conteo).
* `PeriodSummary`: Balance consolidado por rango de fechas en zona `America/Bogota`.
* `BackupPayload`: Estructura JSON completa y versionada para copias de seguridad.

#### 3. Catálogo de Errores Tipados (`com.deyvidjgv.inventario.domain.exception`):
* `DomainException` y `ErrorCode` (`INSUFFICIENT_STOCK`, `NO_OPEN_JORNADA`, `JORNADA_ALREADY_OPEN`, `LOT_NOT_FOUND`, etc.).

#### 4. Puertos de Repositorio y Transaccionalidad (`com.deyvidjgv.inventario.domain.port`):
* `CategoryRepository`, `ProductRepository`, `StockLotRepository` (con ordenamiento FIFO), `JornadaRepository`, `SaleRepository`, `StockAdjustmentRepository`, `ExpenseRepository`, `AuditLogRepository`.
* `TransactionManager`: Contrato para ejecución atómica de operaciones compuestas.

#### 5. Implementación de Servicios de Dominio (`com.deyvidjgv.inventario.domain.service.impl`):
* `InventoryServiceImpl`: Gestión de catálogo, archivado sin borrado y entrada obligatoria con precio de compra.
* `SalesServiceImpl`: Motor de asignación FIFO en ventas, control de jornada única, anulación con reintegro a lotes y cierre con conteo físico.
* `ReportServiceImpl`: Márgenes sobre venta y costo, comparativa viejo vs. nuevo y resúmenes de jornada.
* `ExpenseServiceImpl`: Gastos independientes de la ganancia bruta de mercancía.
* `BackupServiceImpl`: Serialización y restauración atómica en JSON.

---

### C. Validación y Suite de Pruebas Unitarias (JUnit 5)
Se construyeron repositorios en memoria (`InMemoryRepositories`) y una suite de pruebas exhaustiva (`BusinessRulesTest.java`) validando **el 100% de los casos de negocio obligatorios**:

| Caso | Escenario Validado | Resultado |
|---|---|---|
| **Caso A** | Dos lotes (5.000 y 4.300), venta de 30 a 6.000 $\to$ ganancia 34.200, diferencia 16.800, márgenes 16.7%/28.3% (venta) y 20.0%/39.5% (costo). | **PASSED** |
| **Caso B** | "Cerveza Pool" (\$8.000 con costo \$4.300) y "Juego Pool" (\$2.000 sin stock y costo 0). | **PASSED** |
| **Caso C** | Anulación de venta reintegra exactamente 24 unidades al Lote 1 y 6 al Lote 2 con log de auditoría. | **PASSED** |
| **Caso D** | Venta rechazada con `DomainException(INSUFFICIENT_STOCK)` cuando se solicitan 49 de 48 disponibles. | **PASSED** |
| **Caso E** | Conteo al cerrar con faltante (-2) calcula pérdida al costo (\$8.600) y a la venta (\$12.000) y descuenta en FIFO. | **PASSED** |
| **Caso F** | Inmutabilidad de precios históricos tras actualizar el precio del producto en catálogo de \$6.000 a \$6.500. | **PASSED** |
| **Caso G** | Rechazo de venta sin jornada abierta y bloqueo de apertura si ya hay una jornada activa. | **PASSED** |
| **Caso H** | Exportar a JSON, limpiar almacenamiento y restaurar recupera idénticamente todos los datos y balances. | **PASSED** |
| **Gastos** | Ganancia bruta de productos (\$34.200) permanece intacta ante gastos (\$20.000) con ganancia neta informativa (\$14.200). | **PASSED** |

---

### D. Módulo Android `:app` (Persistencia y Presentación)

#### 1. Persistencia Room SQLite (`com.deyvidjgv.inventario.data.local`):
* Entidades con claves foráneas e índices: `CategoryEntity`, `ProductEntity`, `StockLotEntity`, `JornadaEntity`, `SaleEntity`, `SaleLotAllocationEntity`, `StockAdjustmentEntity`, `ExpenseEntity`, `AuditLogEntity`.
* DAOs con consultas especializadas y método `@Transaction` (`insertSaleWithAllocations`).
* `Converters`: TypeConverters para `Instant` (epoch millis) y `LocalDate`.
* `AppDatabase`: Singleton thread-safe SQLite Room (`inventario_pool.db`).
* Adaptadores de repositorio: Conectan los DAOs de Room con las interfaces del dominio.
* `RoomTransactionManager`: Implementa `TransactionManager` mediante `runInTransaction()`.

#### 2. Inyección de Dependencias (`com.deyvidjgv.inventario.di`):
* `AppContainer`: Inicializa la base de datos Room, los repositorios y expone los servicios del dominio.
* `InventarioApplication`: Inicializa `AppContainer` en el ciclo de vida de la aplicación.

#### 3. Interfaz de Usuario y Experiencia Nocturna:
* **Tema y Colores:** Paleta de alto contraste para barra nocturna (`Theme.InventarioPool`, fondo `#121212`, acentos `#00E676` y `#FF9100`).
* **Formateador de Moneda:** `CurrencyFormatter` formatea moneda en pesos enteros colombianos (`$#,##0`).
* **Vistas implementadas:**
  * `MainActivity`: Contenedor principal con Toolbar y `BottomNavigationView`.
  * `HomeFragment`: Panel de estado de jornada (abrir, cerrar, reabrir) y resumen financiero de la noche.
  * `SalesFragment`: Botones grandes por producto para cobro rápido con 1 toque y botón de anulación inmediata.
  * `InventoryFragment`: Visualizador de stock, modal de alta de productos y modal de entrada de mercancía con precio de compra unitario obligatorio.
  * `MarginsFragment`: Desglose de lotes por producto con margen sobre venta, margen sobre costo y análisis comparativo.
  * `ExpensesFragment`: Registro libre de gastos de caja y balance financiero.
  * `BackupFragment`: Exportación vía *Share Sheet* de Android (para compartir por WhatsApp o Drive) e importación de respaldo JSON.

---

### E. Integración Continua y Pipeline del APK (`.github/workflows/android.yml`)
* Workflow configurado para ejecutarse en cada push a `main`:
  1. Configuración de JDK 17 (Temurin).
  2. Ejecución automatizada de pruebas unitarias (`./gradlew :domain:test`).
  3. Compilación del APK Android (`./gradlew :app:assembleDebug`).
  4. Carga y publicación del archivo `.apk` instalable como artefacto descargable en GitHub Actions.

---

## 3. Estado Actual del Repositorio

* **Compilación:** `./gradlew :domain:test` pasa al 100% (9 de 9 pruebas exitosas).
* **Arquitectura:** Totalmente desacoplada bajo principios Hexagonales / Puertos y Adaptadores.
* **Trazabilidad:** Todo el código respeta estrictamente los requerimientos de [`README.md`](../README.md) y [`docs/PLAN_TECNICO.md`](PLAN_TECNICO.md).
