# Inventario Pool — Plan técnico y reparto de trabajo

> La lógica del negocio, explicada en palabras simples y con ejemplos, está en el [`README.md`](../README.md) de la raíz. Léela primero.

App **Android nativa (Java)** para llevar el **inventario y el control de dinero** de un negocio tipo bar con mesas de pool en Colombia. La usa **una sola persona en el celular** (caja o cobrador). Funciona **sin internet** y no debe perder datos.

> El repositorio se llama `Inventario-pwa` por una idea inicial. El proyecto final es una **app Android (APK)**, no una PWA.

Este documento es la fuente de verdad para dividir el trabajo entre dos personas. Si algo del código contradice este documento, se corrige uno de los dos y se avisa al equipo.

---

## 1. Qué hace la app

Solo dos cosas: **inventario** y **dinero**.

| Panel | Para qué sirve |
|---|---|
| **Inventario** | Crear productos, registrar entradas de mercancía con su **precio de compra** y ver el stock. |
| **Vender** | Botones rápidos por categoría. Cada toque suma 1 y descuenta del inventario. |
| **Márgenes** | Por producto: precio de compra de cada lote, ganancia por unidad, margen en %, y la comparación entre lo que se ganó con el precio viejo y lo que se habría ganado con el precio nuevo. |
| **Dinero** | Ventas, costo y ganancia por jornada. Lista libre de gastos. |
| **Jornada** | Botón para abrir y cerrar la noche. Al cerrar se puede contar el stock real. |
| **Respaldo** | Exportar e importar una copia en archivo. Después, sincronización con Firebase. |

### Fuera del alcance (no hacer)

- Fiado, deudas y "me deben".
- Empleados, descuentos por faltante y cobros a personas.
- Reglas especiales para el pool o combos.
- Multi-usuario.

**Pool sin lógica especial.** Todo es un producto con su propio precio, creado por el dueño:
- "Cerveza Águila" a 7.000 y "Cerveza Águila Pool" a 8.000 son **dos productos separados**, cada uno con su stock y sus lotes.
- "Juego Pool" a 2.000 es un producto **sin inventario** (`tracksStock = false`).

---

## 2. Reglas de negocio

Todas las reglas viven en el módulo `:domain` y tienen pruebas.

### 2.1 Dinero y tiempo
- Los montos son **pesos colombianos enteros** (`long`). Nunca `double` ni `float`.
- Las fechas se guardan en UTC (`Instant`, epoch millis) y se muestran en **`America/Bogota`**.
- Los porcentajes se calculan con `double` solo para mostrarlos, con 1 decimal. Nunca se guardan.

### 2.2 Productos
- Tienen nombre, categoría, **precio de venta**, `tracksStock` y `active`.
- **Nunca se borran.** Se archivan (`active = false`) para no romper el historial.
- Cambiar el precio de venta **no cambia** las ventas ya hechas, porque cada venta guarda su `unitPrice`.

### 2.3 Entradas de mercancía (lotes)
- Cada entrada crea un **lote**: producto, cantidad, **precio de compra unitario**, fecha y nota.
- Siempre se pide el precio de compra. No se puede registrar una entrada sin él.
- El stock de un producto es la suma de `quantityRemaining` de sus lotes.

### 2.4 Ventas
- Solo se puede vender con una **jornada abierta**.
- Una venta guarda: producto, cantidad, `unitPrice` (precio de ese momento) y fecha.
- Los productos con stock **consumen lotes en orden FIFO**: primero el lote más viejo (`receivedAt`, y si empatan, el `id` menor).
- Por cada lote tocado se guarda una `SaleLotAllocation` (`saleId`, `lotId`, `quantity`, `unitCost`). De ahí sale el costo real de la venta.
- **No se puede vender más de lo que hay en stock.** La app rechaza la venta y pide registrar primero la entrada.
- Los productos sin stock (`tracksStock = false`) no tocan lotes. Su costo es 0 y su ganancia es el precio completo.

### 2.5 Anular o corregir una venta
- **Anular** devuelve la cantidad a los lotes originales usando las `SaleLotAllocation` y marca la venta como `voided`. No se borra.
- **Corregir** una venta es anular y crear una nueva.
- Toda anulación o edición queda en el `AuditLog`.

### 2.6 Ganancia y márgenes
Por unidad vendida: `ganancia = unitPrice − unitCost` (con el costo del lote del que salió).

Por lote, con `precio = precio de venta actual del producto` y `costo = precio de compra del lote`:

| Métrica | Fórmula |
|---|---|
| Ganancia por unidad | `precio − costo` |
| Margen sobre venta | `(precio − costo) / precio` |
| Margen sobre costo | `(precio − costo) / costo` (si el costo es 0, no se muestra) |

**Comparación viejo contra nuevo**, por producto:
- `gananciaReal` = suma de `(unitPrice − unitCost) × cantidad` de todas las asignaciones de ventas no anuladas.
- `gananciaConCostoNuevo` = suma de `(unitPrice − costoDelLoteMásReciente) × cantidad` de esas mismas ventas.
- `diferencia` = `gananciaConCostoNuevo − gananciaReal`.
- Lote más reciente = mayor `receivedAt`; si empatan, mayor `id`.

### 2.7 Jornada
- Abrir y cerrar son botones manuales. No hay corte automático a medianoche. Una jornada puede cruzar la medianoche.
- Solo puede haber **una jornada abierta** a la vez.
- Se puede **reabrir la última jornada cerrada**.
- Resumen de jornada (sobre ventas no anuladas):
  - `ventas` = suma de `unitPrice × cantidad`.
  - `costo` = suma de `unitCost × cantidad` de las asignaciones.
  - `ganancia` = `ventas − costo`.
  - Los **gastos se muestran aparte** y no modifican la ganancia. Se puede mostrar `ganancia − gastos` como dato informativo.

### 2.8 Conteo al cerrar (opcional)
- Al cerrar, el usuario puede escribir el stock real de cada producto.
- `diferencia = contado − esperado` (el esperado es la suma de lotes).
- **Si falta:** se descuenta de los lotes en FIFO. La pérdida se muestra al costo y al precio de venta.
- **Si sobra:** se crea un lote nuevo con el precio de compra del lote más reciente.
- Se guarda como `StockAdjustment`. **No se le cobra a nadie.**

### 2.9 Gastos
- Concepto libre, monto y fecha. Se puede ligar a una jornada.
- Hay total por jornada y por rango de fechas.

---

## 3. Modelo de datos

| Entidad | Campos principales |
|---|---|
| `Category` | `id`, `name` |
| `Product` | `id`, `name`, `categoryId`, `salePrice` (long), `tracksStock`, `active` |
| `StockLot` | `id`, `productId`, `receivedAt`, `quantityIn`, `quantityRemaining`, `unitCost` (long), `note` |
| `Jornada` | `id`, `openedAt`, `closedAt` (nulo si está abierta) |
| `Sale` | `id`, `jornadaId`, `productId`, `quantity`, `unitPrice` (long), `createdAt`, `voided` |
| `SaleLotAllocation` | `saleId`, `lotId`, `quantity`, `unitCost` (long) |
| `StockAdjustment` | `id`, `productId`, `jornadaId`, `expected`, `counted`, `difference`, `createdAt` |
| `Expense` | `id`, `concept`, `amount` (long), `createdAt`, `jornadaId` (opcional) |
| `AuditLog` | `id`, `entity`, `entityId`, `action`, `beforeJson`, `afterJson`, `createdAt` |

Todas las operaciones que tocan varias tablas (vender, anular, cerrar con conteo) van **dentro de una transacción**. Si algo falla, no queda nada a medias.

---

## 4. Arquitectura

```
Inventario-pwa/
├── domain/        módulo Java puro (sin Android). Reglas, modelos, interfaces y pruebas JUnit.
├── app/           módulo Android. Room, repositorios, pantallas, ViewModels.
├── .github/workflows/android.yml   compila el APK
└── README.md
```

- **Lenguaje:** Java 17.
- **Android:** `minSdk 26`, `targetSdk` el vigente, AndroidX, Material Components y layouts XML.
- **Base de datos:** Room (SQLite).
- **UI:** `ViewModel` + `LiveData`. Una sola Activity con navegación por fragments.
- **Pruebas:** JUnit 5 en `:domain`. La lógica de dinero nunca se escribe dentro de una pantalla.
- **Paquete base sugerido:** `com.deyvidjgv.inventario`.

### Contrato entre las dos mitades

`:domain` define estas interfaces. La persona de datos las implementa con Room y la persona de la app las usa. La app puede avanzar con implementaciones falsas (mocks) mientras no existan las reales.

```java
public interface InventoryService {
    Product createProduct(String name, long categoryId, long salePrice, boolean tracksStock);
    void updateProduct(Product product);
    void archiveProduct(long productId);
    StockLot receiveStock(long productId, int quantity, long unitCost, Instant at, String note);
    List<ProductStock> listStock();
}

public interface SalesService {
    Jornada openJornada(Instant at);
    JornadaSummary closeJornada(long jornadaId, Instant at, Map<Long, Integer> countedByProduct); // el mapa puede ir vacío
    Jornada reopenLastJornada();
    Sale sell(long productId, int quantity);   // exige jornada abierta; usa el precio de venta actual
    void voidSale(long saleId);
}

public interface ReportService {
    ProductMarginReport margins(long productId);
    List<ProductMarginReport> allMargins();
    JornadaSummary summary(long jornadaId);
    PeriodSummary summary(LocalDate from, LocalDate to);   // zona America/Bogota
}

public interface ExpenseService {
    Expense add(String concept, long amount, Instant at, Long jornadaId);
    void delete(long expenseId);
    List<Expense> list(LocalDate from, LocalDate to);
}

public interface BackupService {
    String exportJson();
    void importJson(String json);   // reemplaza todo; validar antes de borrar
}
```

Los errores de negocio (stock insuficiente, no hay jornada abierta, precio de compra faltante) se lanzan como excepciones propias del dominio (`DomainException` con un código), para que la pantalla muestre un mensaje claro.

---

## 5. Pantallas

1. **Inicio:** estado de la jornada (abrir, cerrar o reabrir) y el resumen del momento.
2. **Vender:** categorías arriba y una cuadrícula de botones grandes. Un toque suma 1, hay mantener-pulsado para escribir la cantidad, y se ve el total de la jornada. Se puede anular una venta desde el historial.
3. **Inventario:** lista de productos con stock. Crear, editar y archivar. Botón de **entrada de mercancía** con cantidad y precio de compra.
4. **Márgenes:** lista de productos y, al entrar, los lotes con ganancia por unidad, margen sobre venta y sobre costo, y la comparación viejo contra nuevo.
5. **Dinero:** resumen por jornada y por rango, y lista de gastos.
6. **Cierre:** conteo opcional por producto, diferencias y resumen final.
7. **Respaldo:** exportar e importar el archivo.

Pensar en celular: botones grandes, una mano y letra legible con poca luz. Los montos se muestran con puntos de miles, por ejemplo `600.000`, usando `es-CO` y sin decimales.

---

## 6. Casos de prueba obligatorios

Estos casos salen del negocio real. Deben existir como pruebas automáticas en `:domain`.

**A. Dos lotes y venta que cruza ambos** (Águila, venta a 6.000)
- Lote 1: 24 unidades a 5.000. Lote 2: 24 unidades a 4.300.
- Vender 30: 24 salen del lote 1 y 6 del lote 2.
- Ganancia real = 24×1.000 + 6×1.700 = **34.200**.
- Con el costo nuevo habría sido 30×1.700 = 51.000, así que la diferencia es **16.800**.
- Margen sobre venta: lote 1 = 16,7 %, lote 2 = 28,3 %. Margen sobre costo: lote 1 = 20,0 %, lote 2 = 39,5 %.

**B. Productos del pool, separados**
- "Cerveza Águila Pool" a 8.000 con lote a 4.300 → ganancia por unidad **3.700**.
- "Juego Pool" a 2.000 sin stock → ganancia por unidad **2.000**, y no toca lotes.

**C. Anular una venta**
- Después del caso A, anular la venta: el lote 1 vuelve a 24 y el lote 2 a 24. La venta queda marcada y en el `AuditLog`.

**D. Stock insuficiente**
- Con 48 unidades en total, vender 49 se rechaza y no cambia nada.

**E. Conteo que da faltante**
- Después de vender 30 (caso A) quedan 18 en el lote 2. Se cuentan 16.
- Diferencia **−2**, descontada del lote 2: pérdida al costo **8.600**, al precio de venta **12.000**.

**F. Precio cambiado después**
- Vender a 6.000, subir el producto a 6.500 y vender de nuevo. Las ventas viejas siguen a 6.000.

**G. Una sola jornada abierta**
- Abrir una segunda jornada con una abierta debe fallar. Vender sin jornada abierta debe fallar.

**H. Respaldo**
- Exportar, borrar todo e importar deja los mismos productos, lotes, ventas y gastos, con los mismos totales.

---

## 7. Reparto del trabajo

### Persona A — Dominio y datos
1. Modelos y las interfaces de la sección 4 en `:domain`.
2. Lógica FIFO, ventas, anulaciones, márgenes, jornada y conteo, con los casos de la sección 6.
3. Room: entidades, DAOs, migraciones, transacciones, y las implementaciones de las interfaces.
4. `BackupService` (exportar e importar JSON).

### Persona B — App y entrega
1. Proyecto Android, navegación y tema.
2. Todas las pantallas de la sección 5, usando las interfaces (con mocks al inicio).
3. GitHub Actions que compila y publica el APK (sección 8).
4. Respaldo en pantalla (compartir el archivo por WhatsApp o Drive) y, al final, Firebase.

### Punto de encuentro
Se hace **primero y juntos**: acordar las interfaces y los modelos de la sección 4 y subirlos a `main`. Con eso ninguno espera al otro.

---

## 8. Entrega del APK

El APK lo compila **GitHub Actions** en cada push a `main` y el resultado se descarga desde la pestaña *Actions* o *Releases*. Esqueleto del flujo:

```yaml
name: android
on:
  push:
    branches: [main]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - run: ./gradlew :domain:test :app:assembleRelease
      - uses: actions/upload-artifact@v4
        with: { name: app-release, path: app/build/outputs/apk/release/*.apk }
```

**Firma (muy importante).**
- Las actualizaciones deben firmarse **siempre con la misma llave**. Si la llave cambia, Android obliga a desinstalar la app y **se pierden los datos**.
- Generar la llave una sola vez y guardar copia fuera del repositorio.
- **Nunca** subir el archivo de la llave al repositorio. Se guarda en *Secrets* de GitHub: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Para instalar en el celular hay que permitir "instalar apps de orígenes desconocidos".

---

## 9. Que los datos no se pierdan

1. **Local:** Room con transacciones. Es la fuente de verdad.
2. **Archivo:** exportar a JSON y compartirlo. Recomendar al dueño hacerlo al cerrar la semana.
3. **Firebase (fase final):** sincronización de lo local hacia la nube.
   - Firestore con persistencia sin conexión.
   - Autenticación anónima o con Google.
   - El dueño crea el proyecto y entrega `google-services.json`. **No subir claves privadas al repositorio.**
   - La estrategia de conflictos se define al empezar esa fase.
4. Al desinstalar la app se borran los datos locales. Sin copia en archivo o en Firebase, no hay forma de recuperarlos.

---

## 10. Fases y criterios de listo

| Fase | Contenido | Está listo cuando |
|---|---|---|
| 0 | Interfaces y modelos acordados en `main` | Los dos compilan contra las mismas interfaces |
| 1 | Dominio con FIFO, ventas, márgenes, jornada y conteo | Pasan todos los casos de la sección 6 (menos H) |
| 2 | Room y repositorios | Los mismos casos pasan contra la base real |
| 3 | Pantallas (en paralelo con 1 y 2) | Se puede abrir jornada, vender, anular y cerrar en el celular |
| 4 | APK por GitHub Actions | Un push a `main` deja un APK instalable |
| 5 | Respaldo en archivo | Pasa el caso H |
| 6 | Firebase | Se restaura en otro celular desde la nube |

---

## 11. Convenciones de trabajo

- Ramas: `a/<tema>` y `b/<tema>`. Todo entra a `main` con Pull Request revisado por la otra persona.
- Commits en español, cortos y claros.
- Ningún cálculo de dinero fuera de `:domain`.
- Todo cambio de reglas se refleja primero en este README.
- Nunca subir llaves, contraseñas ni `google-services.json` con claves privadas.

---

## 12. Decisiones ya tomadas

- Pool sin lógica especial: productos separados, cada uno con su precio.
- Sin empleados ni cobros por faltantes. El conteo solo ajusta el stock.
- Sin fiado ni deudas.
- Jornada manual, no por reloj.
- Venta rechazada si no hay stock suficiente.
- Los ajustes por sobrante entran con el costo del lote más reciente.

## 13. Pendiente de confirmar con el cliente

- Si quiere ver el margen en % sobre venta, sobre costo, o ambos como pantalla principal (por ahora, ambos).
- Si el conteo al cerrar debe ser obligatorio o seguir siendo opcional (por ahora, opcional).
- Si el stock insuficiente debe bloquear la venta (por ahora, bloquea) o dejarla pasar con una advertencia.
