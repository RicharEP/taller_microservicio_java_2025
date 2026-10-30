package com.homerios.msnegocioproducto.service;

import java.util.List;

import com.homerios.msnegocioproducto.dto.MovimientoRequest;
import com.homerios.msnegocioproducto.dto.MovimientoResponse;
import com.homerios.msnegocioproducto.dto.StockResponse;

public interface InventarioService {

	MovimientoResponse registrarEntrada(MovimientoRequest request);

	MovimientoResponse registrarSalida(MovimientoRequest request);

	StockResponse consultarStock(Long productoId);

	List<StockResponse> listarStockBajo();

	List<MovimientoResponse> listarMovimientos(Long productoId);
}
