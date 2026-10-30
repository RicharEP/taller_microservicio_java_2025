package com.homerios.msnegocioproducto.dto;

import java.time.LocalDateTime;

import com.homerios.msnegocioproducto.model.TipoMovimiento;

public record MovimientoResponse(
		Long id,
		Long productoId,
		String codigoProducto,
		TipoMovimiento tipo,
		int cantidad,
		int stockResultante,
		String motivo,
		LocalDateTime fechaRegistro) {
}
