package com.homerios.msnegocioproducto.service;

import java.util.List;

import com.homerios.msnegocioproducto.dto.ProductoRequest;
import com.homerios.msnegocioproducto.dto.ProductoResponse;

public interface ProductoService {

	List<ProductoResponse> listar();

	ProductoResponse buscarPorId(Long id);

	ProductoResponse crear(ProductoRequest request);

	ProductoResponse actualizar(Long id, ProductoRequest request);

	/** Baja lógica: conserva el historial de movimientos. */
	void desactivar(Long id);
}
