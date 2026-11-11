package com.homerios.msnegocioproducto.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homerios.msnegocioproducto.config.InventarioProperties;
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

	private static final Logger log = LoggerFactory.getLogger(InventarioServiceImpl.class);

	private final ProductoRepository productoRepository;
	private final MovimientoInventarioRepository movimientoRepository;
	private final ProductoMapper productoMapper;
	private final MovimientoMapper movimientoMapper;
	private final InventarioProperties inventarioProperties;

	public InventarioServiceImpl(ProductoRepository productoRepository,
			MovimientoInventarioRepository movimientoRepository, ProductoMapper productoMapper,
			MovimientoMapper movimientoMapper, InventarioProperties inventarioProperties) {
		this.productoRepository = productoRepository;
		this.movimientoRepository = movimientoRepository;
		this.productoMapper = productoMapper;
		this.movimientoMapper = movimientoMapper;
		this.inventarioProperties = inventarioProperties;
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
		if (request.cantidad() > inventarioProperties.getMaxUnidadesPorMovimiento()) {
			throw new ReglaNegocioException("Un movimiento no puede superar "
					+ inventarioProperties.getMaxUnidadesPorMovimiento() + " unidades");
		}
		Producto producto = obtenerProducto(request.productoId());
		if (!producto.isActivo()) {
			throw new ReglaNegocioException("El producto " + producto.getCodigo() + " está inactivo");
		}
		tipo.aplicar(producto, request.cantidad());
		MovimientoInventario movimiento = new MovimientoInventario(producto, tipo, request.cantidad(),
				request.motivo());
		MovimientoInventario guardado = movimientoRepository.save(movimiento);

		if (tipo == TipoMovimiento.SALIDA && inventarioProperties.isAlertaStockBajo() && producto.tieneStockBajo()) {
			log.warn("ALERTA STOCK BAJO: Producto id={}, codigo='{}', nombre='{}' ha quedado con stock actual={}, inferior al minimo={}",
					producto.getId(), producto.getCodigo(), producto.getNombre(), producto.getStockActual(), producto.getStockMinimo());
		}

		return movimientoMapper.toResponse(guardado);
	}

	private Producto obtenerProducto(Long id) {
		return productoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
	}
}
