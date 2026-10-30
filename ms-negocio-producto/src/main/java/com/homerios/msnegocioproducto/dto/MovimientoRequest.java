package com.homerios.msnegocioproducto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MovimientoRequest(
		@NotNull Long productoId,
		@Min(1) int cantidad,
		@Size(max = 255) String motivo) {
}
