package com.homerios.msnegocioproducto.exception;

public class StockInsuficienteException extends ReglaNegocioException {

	private static final long serialVersionUID = 1L;

	public StockInsuficienteException(String codigo, int disponible, int solicitado) {
		super("Stock insuficiente para el producto " + codigo + ": disponible " + disponible + ", solicitado "
				+ solicitado);
	}
}
