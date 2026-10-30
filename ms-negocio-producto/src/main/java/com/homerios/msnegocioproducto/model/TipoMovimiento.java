package com.homerios.msnegocioproducto.model;

/**
 * Tipo de movimiento de inventario. Cada tipo sabe cómo afectar el stock,
 * de modo que agregar un nuevo tipo (p. ej. AJUSTE) no modifica el servicio.
 */
public enum TipoMovimiento {

	ENTRADA {
		@Override
		public void aplicar(Producto producto, int cantidad) {
			producto.incrementarStock(cantidad);
		}
	},
	SALIDA {
		@Override
		public void aplicar(Producto producto, int cantidad) {
			producto.disminuirStock(cantidad);
		}
	};

	public abstract void aplicar(Producto producto, int cantidad);
}
