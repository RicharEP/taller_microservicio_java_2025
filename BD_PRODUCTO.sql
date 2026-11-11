-- Ejecutar conectado a BD_PRODUCTO. Docker Compose crea esa base mediante POSTGRES_DB.
-- En PostgreSQL local: crear primero la base con createdb y luego aplicar este archivo.

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
