package com.homerios.msnegocioproducto.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
		LocalDateTime timestamp,
		int status,
		String mensaje,
		List<String> detalles) {

	public static ErrorResponse of(int status, String mensaje) {
		return new ErrorResponse(LocalDateTime.now(), status, mensaje, List.of());
	}

	public static ErrorResponse of(int status, String mensaje, List<String> detalles) {
		return new ErrorResponse(LocalDateTime.now(), status, mensaje, detalles);
	}
}
