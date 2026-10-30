package com.homerios.msnegocioproducto.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homerios.msnegocioproducto.dto.ProductoRequest;
import com.homerios.msnegocioproducto.dto.ProductoResponse;
import com.homerios.msnegocioproducto.exception.RecursoNoEncontradoException;
import com.homerios.msnegocioproducto.exception.ReglaNegocioException;
import com.homerios.msnegocioproducto.mapper.ProductoMapper;
import com.homerios.msnegocioproducto.model.Producto;
import com.homerios.msnegocioproducto.repository.ProductoRepository;
import com.homerios.msnegocioproducto.service.ProductoService;

@Service
@Transactional
public class ProductoServiceImpl implements ProductoService {

	private final ProductoRepository productoRepository;
	private final ProductoMapper productoMapper;

	public ProductoServiceImpl(ProductoRepository productoRepository, ProductoMapper productoMapper) {
		this.productoRepository = productoRepository;
		this.productoMapper = productoMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductoResponse> listar() {
		return productoRepository.findByActivoTrue().stream().map(productoMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ProductoResponse buscarPorId(Long id) {
		return productoMapper.toResponse(obtener(id));
	}

	@Override
	public ProductoResponse crear(ProductoRequest request) {
		if (productoRepository.existsByCodigo(request.codigo())) {
			throw new ReglaNegocioException("Ya existe un producto con el código " + request.codigo());
		}
		Producto guardado = productoRepository.save(productoMapper.toEntity(request));
		return productoMapper.toResponse(guardado);
	}

	@Override
	public ProductoResponse actualizar(Long id, ProductoRequest request) {
		Producto producto = obtener(id);
		if (!producto.getCodigo().equals(request.codigo())) {
			throw new ReglaNegocioException("El código del producto no puede modificarse");
		}
		producto.actualizarDatos(request.nombre(), request.descripcion(), request.precio(), request.stockMinimo());
		return productoMapper.toResponse(producto);
	}

	@Override
	public void desactivar(Long id) {
		obtener(id).desactivar();
	}

	private Producto obtener(Long id) {
		return productoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
	}
}
