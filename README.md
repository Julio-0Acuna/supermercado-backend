# 🏪 Sistema de Gestión de Supermercado

Sistema backend desarrollado en Java (JDBC) y MySQL para la gestión de productos, ventas y control automático de inventario en un supermercado.

El proyecto implementa reglas de negocio directamente en la base de datos mediante triggers y procedimientos almacenados, garantizando integridad y consistencia de los datos.

---

## 🚀 Funcionalidades

### 📦 Gestión de Productos
- CRUD completo (Crear, Leer, Actualizar, Eliminar lógico)
- Validación de precio y stock
- Control de estado mediante campo "activo"

### 🧾 Gestión de Boletas
- Creación de boletas con múltiples productos
- Cálculo automático de:
  - Subtotal
  - Descuento
  - IVA (19%)
  - Total
  - Vuelto
- Registro de método de pago

### 📉 Control Automático de Inventario
- Descuento automático de stock al vender
- Validación de stock insuficiente
- Ajuste de stock al modificar o eliminar detalle

---

## ⚙ Lógica Automatizada en MySQL

La base de datos incluye:

- Triggers BEFORE y AFTER
- Procedimiento almacenado sp_recalcular_boleta
- Restricciones CHECK
- FOREIGN KEY
- UNIQUE
- Eliminación lógica

Esto permite que la lógica crítica del negocio esté protegida directamente en el motor de base de datos.

---

## 🛠 Tecnologías Utilizadas

- Java (JDK 17+)
- JDBC
- MySQL 8+
- NetBeans (Java Ant)
- MySQL Workbench

---

## 🏗 Arquitectura del Proyecto

src/
 ├── app/        → Pruebas y ejecución
 ├── dao/        → Acceso a datos (JDBC)
 ├── db/         → Conexión a base de datos
 ├── model/      → Entidades
 └── service/    → Lógica de negocio

El sistema está organizado en capas para separar responsabilidades y facilitar mantenimiento.

---

## 🧪 Pruebas Implementadas

Se realizaron pruebas automatizadas desde Java para validar:

- Correcto funcionamiento del CRUD
- Descuento automático de stock
- Recalculo correcto de subtotal, IVA y total
- Manejo adecuado de errores esperados (stock insuficiente)

---

## 🎯 Objetivo del Proyecto

Aplicar buenas prácticas de desarrollo backend integrando:

- Programación orientada a objetos
- Persistencia de datos con JDBC
- Diseño relacional
- Automatización de reglas de negocio
- Manejo de excepciones
- Arquitectura en capas

---

## 👨‍💻 Autor

Desarrollado por Julio Acuña como proyecto académico y práctica avanzada en desarrollo backend con Java y MySQL.
