package com.homerios.msnegocioproducto.mapper;

import org.springframework.stereotype.Component;

import com.homerios.msnegocioproducto.dto.MovimientoResponse;
import com.homerios.msnegocioproducto.model.MovimientoInventario;

@Component
public class MovimientoMapper {

	public MovimientoResponse toResponse(MovimientoInventario m) {
		return new MovimientoResponse(m.getId(), m.getProducto().getId(), m.getProducto().getCodigo(), m.getTipo(),
				m.getCantidad(), m.getStockResultante(), m.getMotivo(), m.getFechaRegistro());
	}
}
