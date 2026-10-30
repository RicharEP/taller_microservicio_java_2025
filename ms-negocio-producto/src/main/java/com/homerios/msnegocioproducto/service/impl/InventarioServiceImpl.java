package com.homerios.msnegocioproducto.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homerios.msnegocioproducto.dto.MovimientoRequest;
import com.homerios.msnegocioproducto.dto.MovimientoResponse;
import com.homerios.msnegocioproducto.dto.StockResponse;
import com.homerios.msnegocioproducto.exception.RecursoNoEncontradoException;
import com.homerios.msnegocioproducto.exception.ReglaNegocioException;
import com.homerios.msnegocioproducto.mapper.MovimientoMapper;
import com.homerios.msnegocioproducto.mapper.ProductoMapper;
import com.homerios.msnegocioproducto.model.MovimientoInventario;
import com.homerios.msnegocioproducto.model.Producto;
import com.homerios.msnegocioproducto.model.TipoMovimiento;
import com.homerios.msnegocioproducto.repository.MovimientoInventarioRepository;
import com.homerios.msnegocioproducto.repository.ProductoRepository;
import com.homerios.msnegocioproducto.service.InventarioService;

@Service
@Transactional
public class InventarioServiceImpl implements InventarioService {

	private final ProductoRepository productoRepository;
	private final MovimientoInventarioRepository movimientoRepository;
	private final ProductoMapper productoMapper;
	private final MovimientoMapper movimientoMapper;

	public InventarioServiceImpl(ProductoRepository productoRepository,
			MovimientoInventarioRepository movimientoRepository, ProductoMapper productoMapper,
			MovimientoMapper movimientoMapper) {
		this.productoRepository = productoRepository;
		this.movimientoRepository = movimientoRepository;
		this.productoMapper = productoMapper;
		this.movimientoMapper = movimientoMapper;
	}

	@Override
	public MovimientoResponse registrarEntrada(MovimientoRequest request) {
		return registrar(TipoMovimiento.ENTRADA, request);
	}

	@Override
	public MovimientoResponse registrarSalida(MovimientoRequest request) {
		return registrar(TipoMovimiento.SALIDA, request);
	}

	@Override
	@Transactional(readOnly = true)
	public StockResponse consultarStock(Long productoId) {
		return productoMapper.toStockResponse(obtenerProducto(productoId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StockResponse> listarStockBajo() {
		return productoRepository.findConStockBajo().stream().map(productoMapper::toStockResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MovimientoResponse> listarMovimientos(Long productoId) {
		obtenerProducto(productoId);
		return movimientoRepository.findByProductoIdOrderByFechaRegistroDesc(productoId).stream()
				.map(movimientoMapper::toResponse).toList();
	}

	private MovimientoResponse registrar(TipoMovimiento tipo, MovimientoRequest request) {
		Producto producto = obtenerProducto(request.productoId());
		if (!producto.isActivo()) {
			throw new ReglaNegocioException("El producto " + producto.getCodigo() + " está inactivo");
		}
		tipo.aplicar(producto, request.cantidad());
		MovimientoInventario movimiento = new MovimientoInventario(producto, tipo, request.cantidad(),
				request.motivo());
		return movimientoMapper.toResponse(movimientoRepository.save(movimiento));
	}

	private Producto obtenerProducto(Long id) {
		return productoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
	}
}
