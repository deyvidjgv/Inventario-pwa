# Inventario Pool — Plan de proyecto

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
