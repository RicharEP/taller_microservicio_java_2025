package com.homerios.msnegocioproducto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.homerios.msnegocioproducto.exception.StockInsuficienteException;
import com.homerios.msnegocioproducto.model.Producto;
import com.homerios.msnegocioproducto.model.TipoMovimiento;

class ProductoTest {

	private Producto nuevoProducto() {
		return new Producto("P-001", "Laptop", null, new BigDecimal("2500.00"), 2);
	}

	@Test
	void entradaIncrementaStock() {
		Producto p = nuevoProducto();
		TipoMovimiento.ENTRADA.aplicar(p, 10);
		assertEquals(10, p.getStockActual());
	}

	@Test
	void salidaDisminuyeStock() {
		Producto p = nuevoProducto();
		TipoMovimiento.ENTRADA.aplicar(p, 10);
		TipoMovimiento.SALIDA.aplicar(p, 4);
		assertEquals(6, p.getStockActual());
	}

	@Test
	void salidaMayorAlStockLanzaExcepcion() {
		Producto p = nuevoProducto();
		TipoMovimiento.ENTRADA.aplicar(p, 3);
		assertThrows(StockInsuficienteException.class, () -> TipoMovimiento.SALIDA.aplicar(p, 4));
		assertEquals(3, p.getStockActual());
	}

	@Test
	void detectaStockBajo() {
		Producto p = nuevoProducto();
		TipoMovimiento.ENTRADA.aplicar(p, 5);
		TipoMovimiento.SALIDA.aplicar(p, 3);
		assertTrue(p.tieneStockBajo());
	}
}
