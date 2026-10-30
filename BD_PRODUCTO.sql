-- Database: BD_PRODUCTO
-- Ejecutar el bloque CREATE DATABASE conectado a la BD "postgres";
-- luego conectarse a BD_PRODUCTO para ejecutar el resto.

-- DROP DATABASE IF EXISTS "BD_PRODUCTO";

CREATE DATABASE "BD_PRODUCTO"
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'Spanish_Peru.1252'
    LC_CTYPE = 'Spanish_Peru.1252'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;

-- ===== Conectarse a BD_PRODUCTO a partir de aqui =====

CREATE SCHEMA IF NOT EXISTS sys;

-- Producto: el stock_actual es el saldo vigente; se modifica solo mediante movimientos.
CREATE TABLE IF NOT EXISTS sys.producto
(
    id_producto  BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(30)    NOT NULL,
    nombre       VARCHAR(150)   NOT NULL,
    descripcion  VARCHAR(255),
    precio       NUMERIC(12, 2) NOT NULL,
    stock_actual INTEGER        NOT NULL DEFAULT 0,
    stock_minimo INTEGER        NOT NULL DEFAULT 0,
    activo       BOOLEAN        NOT NULL DEFAULT TRUE,
    version      BIGINT         NOT NULL DEFAULT 0, -- bloqueo optimista
    CONSTRAINT uq_producto_codigo UNIQUE (codigo),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0),
    CONSTRAINT ck_producto_stock_actual CHECK (stock_actual >= 0),
    CONSTRAINT ck_producto_stock_minimo CHECK (stock_minimo >= 0)
);

-- Kardex: historial inmutable de ingresos y salidas.
CREATE TABLE IF NOT EXISTS sys.movimiento_inventario
(
    id_movimiento   BIGSERIAL PRIMARY KEY,
    id_producto     BIGINT       NOT NULL REFERENCES sys.producto (id_producto),
    tipo            VARCHAR(10)  NOT NULL,
    cantidad        INTEGER      NOT NULL,
    stock_resultante INTEGER     NOT NULL,
    motivo          VARCHAR(255),
    fecha_registro  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA')),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad > 0)
);

CREATE INDEX IF NOT EXISTS ix_movimiento_producto_fecha
    ON sys.movimiento_inventario (id_producto, fecha_registro DESC);
