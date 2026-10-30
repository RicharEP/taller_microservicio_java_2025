package com.homerios.msnegocioproducto.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
		@NotBlank @Size(max = 30) String codigo,
		@NotBlank @Size(max = 150) String nombre,
		@Size(max = 255) String descripcion,
		@NotNull @DecimalMin("0.00") BigDecimal precio,
		@Min(0) int stockMinimo) {
}
