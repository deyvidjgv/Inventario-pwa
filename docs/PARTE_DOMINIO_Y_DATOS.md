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

- Entidades y DAOs de **Room (SQLite)**.
- **Transacciones** en todo lo que toca varias tablas (vender, anular, cerrar con conteo), para que nada quede a medias.
- Implementaciones de las interfaces anteriores sobre Room.
- Migraciones de la base de datos cuando el modelo cambie.
- Registro de auditoría de cada edición o anulación.

### 4. Respaldo

- `BackupService`: exportar a un archivo JSON y restaurarlo, validando el archivo antes de borrar nada.
- La pantalla de respaldo y el botón para compartir el archivo los hace la otra persona.

## Lo que NO hago

- Pantallas, navegación, diseño ni colores.
- GitHub Actions ni la firma del APK.
- Firebase (se hace al final, entre los dos).

## Cómo me conecto con la otra persona

1. **Primero acordamos juntos** las interfaces y los modelos y los subimos a `main` (Fase 0). Después cada uno avanza por su lado.
2. Yo dejo en `:domain` una implementación **de mentira** (`Fake…Service`) con datos de ejemplo. Así la app se puede probar sin esperar a la base de datos real.
3. Cuando una regla cambie, **primero se cambia el README** y luego el código.
4. Trabajo en ramas `a/<tema>` y todo entra a `main` por Pull Request que revisa la otra persona.

## Qué necesito de la otra persona

- Que use **solo las interfaces** de `:domain`, sin hacer cuentas de dinero en las pantallas.
- Que me avise si una pantalla necesita un dato que las interfaces no dan (por ejemplo, un resumen distinto). Lo agrego yo.
- Que revise mis Pull Requests y yo los suyos.

## Orden de trabajo

- [ ] **Fase 0:** modelos e interfaces acordados y subidos a `main` (junto con la otra persona).
- [ ] **Fase 1:** lógica de inventario, ventas, márgenes, jornada y conteo con los casos A a G pasando.
- [ ] **Fase 1b:** implementaciones `Fake…Service` para que la app avance en paralelo.
- [ ] **Fase 2:** Room, DAOs, transacciones y migraciones. Los mismos casos pasan contra la base real.
- [ ] **Fase 5:** `BackupService` con el caso H pasando.

Listo para entregar una fase significa: **compila, las pruebas pasan y el README sigue diciendo lo mismo que hace el código.**
