package com.homerios.msnegocioproducto.dto;

public record StockResponse(
		Long productoId,
		String codigo,
		String nombre,
		int stockActual,
		int stockMinimo,
		boolean stockBajo) {
}
