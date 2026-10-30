package com.homerios.msnegocioproducto.exception;

/** Violación de una regla de negocio (p. ej. código duplicado, producto inactivo). */
public class ReglaNegocioException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ReglaNegocioException(String mensaje) {
		super(mensaje);
	}
}
