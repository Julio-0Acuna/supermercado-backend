
DROP DATABASE IF EXISTS supermercado;
CREATE DATABASE supermercado
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE supermercado;

CREATE TABLE producto (
  id_producto     INT AUTO_INCREMENT PRIMARY KEY,
  codigo_barra    VARCHAR(50) NOT NULL,
  nombre          VARCHAR(120) NOT NULL,
  precio          DECIMAL(10,2) NOT NULL,
  stock           INT NOT NULL DEFAULT 0,
  activo          TINYINT(1) NOT NULL DEFAULT 1,
  creado_en       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en  DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT uq_producto_codigo UNIQUE (codigo_barra),
  CONSTRAINT ck_producto_precio CHECK (precio >= 0),
  CONSTRAINT ck_producto_stock  CHECK (stock >= 0)
);

CREATE INDEX idx_producto_nombre ON producto(nombre);

CREATE TABLE boleta (
  id_boleta       INT AUTO_INCREMENT PRIMARY KEY,
  folio           VARCHAR(30) NOT NULL,
  fecha           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- Resumen
  subtotal        DECIMAL(10,2) NOT NULL DEFAULT 0,
  descuento       DECIMAL(10,2) NOT NULL DEFAULT 0,
  iva             DECIMAL(10,2) NOT NULL DEFAULT 0,
  total           DECIMAL(10,2) NOT NULL DEFAULT 0,

  -- Pago
  metodo_pago     ENUM('EFECTIVO','DEBITO','CREDITO','TRANSFERENCIA') NOT NULL,
  monto_pagado    DECIMAL(10,2) NOT NULL DEFAULT 0,
  vuelto          DECIMAL(10,2) NOT NULL DEFAULT 0,

  -- Estado
  estado          ENUM('EMITIDA','ANULADA') NOT NULL DEFAULT 'EMITIDA',

  CONSTRAINT uq_boleta_folio UNIQUE (folio),
  CONSTRAINT ck_boleta_descuento CHECK (descuento >= 0),
  CONSTRAINT ck_boleta_monto_pagado CHECK (monto_pagado >= 0),
  CONSTRAINT ck_boleta_totales CHECK (subtotal >= 0 AND iva >= 0 AND total >= 0)
);

CREATE INDEX idx_boleta_fecha ON boleta(fecha);
CREATE INDEX idx_boleta_estado ON boleta(estado);


CREATE TABLE boleta_detalle (
  id_detalle      INT AUTO_INCREMENT PRIMARY KEY,
  id_boleta       INT NOT NULL,
  id_producto     INT NOT NULL,

  cantidad        INT NOT NULL,
  precio_unitario DECIMAL(10,2) NOT NULL,
  subtotal_linea  DECIMAL(10,2) NOT NULL,

  CONSTRAINT fk_detalle_boleta
    FOREIGN KEY (id_boleta) REFERENCES boleta(id_boleta)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  CONSTRAINT fk_detalle_producto
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

  CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
  CONSTRAINT ck_detalle_precio_unitario CHECK (precio_unitario >= 0),
  CONSTRAINT ck_detalle_subtotal_linea CHECK (subtotal_linea >= 0)
);

CREATE INDEX idx_detalle_boleta ON boleta_detalle(id_boleta);
CREATE INDEX idx_detalle_producto ON boleta_detalle(id_producto);


DELIMITER $$

CREATE TRIGGER bi_boleta_detalle_calc
BEFORE INSERT ON boleta_detalle
FOR EACH ROW
BEGIN
  SET NEW.subtotal_linea = NEW.cantidad * NEW.precio_unitario;
END$$

DELIMITER ;

DROP TRIGGER IF EXISTS bu_boleta_detalle_calc;

DELIMITER $$

CREATE TRIGGER bu_boleta_detalle_calc
BEFORE UPDATE ON boleta_detalle
FOR EACH ROW
BEGIN
  SET NEW.subtotal_linea = NEW.cantidad * NEW.precio_unitario;
END$$

DELIMITER ;

/* Descuenta stock después de insertar detalle */

DROP TRIGGER IF EXISTS boleta_detalle_stock;

DELIMITER $$

CREATE TRIGGER boleta_detalle_stock
AFTER INSERT ON boleta_detalle
FOR EACH ROW
BEGIN
  DECLARE stock_actual INT DEFAULT 0;

  SELECT stock INTO stock_actual
  FROM producto
  WHERE id_producto = NEW.id_producto
  FOR UPDATE;

  IF stock_actual < NEW.cantidad THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Stock insuficiente para el producto';
  END IF;

  UPDATE producto
  SET stock = stock - NEW.cantidad
  WHERE id_producto = NEW.id_producto;
END$$

DELIMITER ;

/* Si se actualiza el detalle (cambia cantidad o producto), ajusta stock */
DROP TRIGGER IF EXISTS au_boleta_detalle_stock;

DELIMITER $$

CREATE TRIGGER au_boleta_detalle_stock
AFTER UPDATE ON boleta_detalle
FOR EACH ROW
BEGIN
	DECLARE diff INT DEFAULT 0;
  -- Si cambió el producto:
  IF OLD.id_producto <> NEW.id_producto THEN
    -- devolver stock al producto viejo
    UPDATE producto
    SET stock = stock + OLD.cantidad
    WHERE id_producto = OLD.id_producto;

    -- descontar stock del producto nuevo (validando)
    IF (SELECT stock FROM producto WHERE id_producto = NEW.id_producto) < NEW.cantidad THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Stock insuficiente para el producto (update)';
    END IF;

    UPDATE producto
    SET stock = stock - NEW.cantidad
    WHERE id_producto = NEW.id_producto;

  ELSE
    -- mismo producto: ajustar diferencia
    SET diff = NEW.cantidad - OLD.cantidad;

    IF diff > 0 THEN
      IF (SELECT stock FROM producto WHERE id_producto = NEW.id_producto) < diff THEN
        SIGNAL SQLSTATE '45000'
          SET MESSAGE_TEXT = 'Stock insuficiente para aumentar cantidad';
      END IF;

      UPDATE producto
      SET stock = stock - diff
      WHERE id_producto = NEW.id_producto;
    ELSEIF diff < 0 THEN
      UPDATE producto
      SET stock = stock + ABS(diff)
      WHERE id_producto = NEW.id_producto;
    END IF;
  END IF;
END$$

DELIMITER ;

/* Al borrar una línea, devuelve stock */
DROP TRIGGER IF EXISTS ad_boleta_detalle_stock;

DELIMITER $$

CREATE TRIGGER ad_boleta_detalle_stock
AFTER DELETE ON boleta_detalle
FOR EACH ROW
BEGIN
  UPDATE producto
  SET stock = stock + OLD.cantidad
  WHERE id_producto = OLD.id_producto;
END$$

DELIMITER ;

/* Recalcular resumen boleta: AFTER INSERT/UPDATE/DELETE en detalle */
DROP PROCEDURE IF EXISTS sp_recalcular_boleta;

DELIMITER $$

CREATE PROCEDURE sp_recalcular_boleta(IN p_id_boleta INT)
BEGIN
  DECLARE v_subtotal  DECIMAL(10,2) DEFAULT 0;
  DECLARE v_descuento DECIMAL(10,2) DEFAULT 0;
  DECLARE v_iva_rate  DECIMAL(10,4) DEFAULT 0.19;

  -- 1) subtotal = suma de líneas
  SELECT IFNULL(SUM(subtotal_linea), 0)
    INTO v_subtotal
  FROM boleta_detalle
  WHERE id_boleta = p_id_boleta;

  -- 2) descuento de la boleta (si no existe, queda 0)
  SELECT IFNULL(descuento, 0)
    INTO v_descuento
  FROM boleta
  WHERE id_boleta = p_id_boleta;

  -- 3) evitar descuento mayor que subtotal
  IF v_descuento > v_subtotal THEN
    SET v_descuento = v_subtotal;
  END IF;

  -- 4) actualizar resumen
  UPDATE boleta
  SET
    subtotal  = v_subtotal,
    descuento = v_descuento,
    iva       = ROUND((v_subtotal - v_descuento) * v_iva_rate, 2),
    total     = ROUND((v_subtotal - v_descuento) * (1 + v_iva_rate), 2),
    vuelto    = CASE
                  WHEN monto_pagado > 0 THEN ROUND(monto_pagado - ((v_subtotal - v_descuento) * (1 + v_iva_rate)), 2)
                  ELSE 0
                END
  WHERE id_boleta = p_id_boleta;
END$$

DELIMITER ;

DROP TRIGGER IF EXISTS ai_boleta_detalle_recalc;

DELIMITER $$

CREATE TRIGGER ai_boleta_detalle_recalc
AFTER INSERT ON boleta_detalle
FOR EACH ROW
BEGIN
  CALL sp_recalcular_boleta(NEW.id_boleta);
END$$

DELIMITER ;


DROP TRIGGER IF EXISTS au_boleta_detalle_recalc;

DELIMITER $$

CREATE TRIGGER au_boleta_detalle_recalc
AFTER UPDATE ON boleta_detalle
FOR EACH ROW
BEGIN
  CALL sp_recalcular_boleta(NEW.id_boleta);
END$$

DELIMITER ;

DROP TRIGGER IF EXISTS ad_boleta_detalle_recalc;

DELIMITER $$

CREATE TRIGGER ad_boleta_detalle_recalc
AFTER DELETE ON boleta_detalle
FOR EACH ROW
BEGIN
  CALL sp_recalcular_boleta(OLD.id_boleta);
END$$

DELIMITER ;

/* Si se cambia descuento/monto_pagado en boleta, recalcular total/vuelto */
DROP TRIGGER IF EXISTS bu_boleta_recalc;

DELIMITER $$

CREATE TRIGGER bu_boleta_recalc
BEFORE UPDATE ON boleta
FOR EACH ROW
BEGIN
  IF NEW.descuento < 0 THEN
    SET NEW.descuento = 0;
  END IF;
END$$

DELIMITER ;

DELIMITER $$
CREATE TRIGGER au_boleta_recalc
AFTER UPDATE ON boleta
FOR EACH ROW
BEGIN
  -- Recalcula cuando cambian campos relevantes
  IF (OLD.descuento <> NEW.descuento) OR (OLD.monto_pagado <> NEW.monto_pagado) THEN
    CALL sp_recalcular_boleta(NEW.id_boleta);
  END IF;
END$$

DELIMITER ;

/* ============================================================
   VISTAS (para consultas fáciles)
   ============================================================ */

-- 1) Resumen de boletas
CREATE OR REPLACE VIEW vw_boleta_resumen AS
SELECT
  id_boleta, folio, fecha, estado,
  subtotal, descuento, iva, total,
  metodo_pago, monto_pagado, vuelto
FROM boleta;

-- 2) Boleta completa (cabecera + líneas)
CREATE OR REPLACE VIEW vw_boleta_completa AS
SELECT
  b.id_boleta, b.folio, b.fecha, b.estado,
  p.codigo_barra, p.nombre,
  d.cantidad, d.precio_unitario, d.subtotal_linea,
  b.subtotal, b.descuento, b.iva, b.total,
  b.metodo_pago, b.monto_pagado, b.vuelto
FROM boleta b
JOIN boleta_detalle d ON d.id_boleta = b.id_boleta
JOIN producto p ON p.id_producto = d.id_producto;

/* ============================================================
   DATOS DE PRUEBA (OPCIONAL)
   - Puedes borrar esta sección si no la quieres.
   ============================================================ */
INSERT INTO producto (codigo_barra, nombre, precio, stock) VALUES
('780000000001', 'Pan', 1200.00, 50),
('780000000002', 'Leche 1L', 1100.00, 40),
('780000000003', 'Arroz 1Kg', 1600.00, 35),
('780000000004', 'Huevos 12u', 3200.00, 20);

-- Crear una boleta vacía (cabecera)
INSERT INTO boleta (folio, metodo_pago, monto_pagado, descuento)
VALUES ('BOL-000001', 'EFECTIVO', 10000.00, 0);

-- Agregar líneas (detalle) -> triggers calculan y actualizan totales + stock
INSERT INTO boleta_detalle (id_boleta, id_producto, cantidad, precio_unitario, subtotal_linea)
VALUES
(1, 1, 2, 1200.00, 0),
(1, 2, 1, 1100.00, 0),
(1, 4, 1, 3200.00, 0);


/* ==========================================================================*/
SELECT id_producto, stock FROM producto WHERE id_producto = 1;

INSERT INTO boleta_detalle (id_boleta, id_producto, cantidad, precio_unitario, subtotal_linea)
VALUES (1, 1, 1, 1200, 0);

SELECT id_producto, stock FROM producto WHERE id_producto = 1;



SELECT id_boleta, folio, subtotal, descuento, iva, total, monto_pagado, vuelto
FROM boleta
WHERE id_boleta = 1;


SELECT id_detalle, id_producto, cantidad, precio_unitario, subtotal_linea
FROM boleta_detalle
WHERE id_boleta = 1;

INSERT INTO boleta_detalle (id_boleta, id_producto, cantidad, precio_unitario, subtotal_linea)
VALUES (1, 3, 2, 1600.00, 0);

SELECT id_detalle, id_producto, cantidad, precio_unitario, subtotal_linea
FROM boleta_detalle
WHERE id_boleta = 1
ORDER BY id_detalle DESC;

SELECT id_boleta, folio, subtotal, descuento, iva, total, monto_pagado, vuelto
FROM boleta
WHERE id_boleta = 1;


SELECT
  b.id_boleta,
  SUM(d.subtotal_linea) AS subtotal_calculado_desde_detalle,
  b.subtotal AS subtotal_guardado_en_boleta,
  ROUND((b.subtotal - b.descuento) * 0.19, 2) AS iva_esperado,
  b.iva AS iva_guardado,
  ROUND((b.subtotal - b.descuento) * 1.19, 2) AS total_esperado,
  b.total AS total_guardado
FROM boleta b
JOIN boleta_detalle d ON d.id_boleta = b.id_boleta
WHERE b.id_boleta = 1
GROUP BY b.id_boleta;