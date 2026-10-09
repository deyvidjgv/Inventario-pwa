# Mi parte: Dominio y Datos

Este documento explica **qué voy a construir yo** en el proyecto Inventario Pool y qué puede esperar de mí la persona que hace la app. El plan completo del proyecto está en el [`README.md`](../README.md) de la raíz. Si algo de aquí contradice ese plan, manda el README.

## En una frase

Yo construyo **el cerebro de la app**: las reglas de inventario, ventas, ganancias y jornadas, y la base de datos donde se guardan. La otra persona construye **lo que se ve**: las pantallas, la navegación y el APK.

La app es Android nativa en Java y la usa una sola persona en el celular, sin internet.

## Qué entrego

### 1. Módulo `:domain` (Java puro, sin Android)

Aquí vive toda la lógica de dinero. Nada de esto se escribe dentro de una pantalla.

- **Modelos:** `Category`, `Product`, `StockLot`, `Jornada`, `Sale`, `SaleLotAllocation`, `StockAdjustment`, `Expense`, `AuditLog`.
- **Servicios** (las interfaces del README, sección 4):
  - `InventoryService`: crear, editar y archivar productos; registrar entradas con precio de compra; ver el stock.
  - `SalesService`: abrir, cerrar y reabrir jornada; vender; anular ventas.
  - `ReportService`: márgenes por producto, resumen por jornada y por rango de fechas.
  - `ExpenseService`: gastos libres.
  - `BackupService`: exportar e importar todo en JSON.
- **Reglas que implemento** (README, sección 2):
  - Los lotes se consumen primero el más viejo (FIFO) y cada venta guarda de qué lote salió.
  - La ganancia real sale del costo de cada lote, no de un promedio.
  - Márgenes sobre venta y sobre costo, y la comparación entre el precio de compra viejo y el nuevo.
  - Una sola jornada abierta a la vez. Solo se vende con jornada abierta.
  - Anular una venta devuelve las unidades a sus lotes originales.
  - El conteo al cerrar ajusta el stock y no le cobra a nadie.
  - Se rechaza vender más de lo que hay en stock.
  - Los productos nunca se borran, se archivan. Cambiar un precio no toca las ventas ya hechas.
- **Errores de negocio claros:** una `DomainException` con código (stock insuficiente, sin jornada abierta, falta el precio de compra…) para que la pantalla muestre un mensaje entendible.
- **Dinero en pesos enteros** (`long`) y fechas en UTC, mostradas en `America/Bogota`.

### 2. Pruebas automáticas (JUnit 5)

Cada regla se prueba con los casos reales del README, sección 6:

| Caso | Qué comprueba |
|---|---|
| A | Dos lotes (5.000 y 4.300), venta de 30 a 6.000: ganancia 34.200 y diferencia 16.800 |
| B | Productos del pool separados y juego sin stock |
| C | Anular una venta devuelve los lotes |
| D | Stock insuficiente se rechaza |
| E | Conteo con faltante |
| F | Cambio de precio no altera ventas viejas |
| G | Una sola jornada abierta |
| H | Exportar, borrar e importar deja los mismos totales |

### 3. Base de datos en el celular (módulo `:app`, solo la capa de datos)

