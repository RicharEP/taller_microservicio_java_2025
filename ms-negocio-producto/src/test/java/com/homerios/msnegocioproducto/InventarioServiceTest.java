package com.homerios.msnegocioproducto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.homerios.msnegocioproducto.config.InventarioProperties;
import com.homerios.msnegocioproducto.dto.MovimientoRequest;
import com.homerios.msnegocioproducto.dto.MovimientoResponse;
import com.homerios.msnegocioproducto.exception.ReglaNegocioException;
import com.homerios.msnegocioproducto.mapper.MovimientoMapper;
import com.homerios.msnegocioproducto.mapper.ProductoMapper;
import com.homerios.msnegocioproducto.model.MovimientoInventario;
import com.homerios.msnegocioproducto.model.Producto;
import com.homerios.msnegocioproducto.model.TipoMovimiento;
import com.homerios.msnegocioproducto.repository.MovimientoInventarioRepository;
import com.homerios.msnegocioproducto.repository.ProductoRepository;
import com.homerios.msnegocioproducto.service.impl.InventarioServiceImpl;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

	@Mock
	private ProductoRepository productoRepository;

	@Mock
	private MovimientoInventarioRepository movimientoRepository;

	@Mock
	private ProductoMapper productoMapper;

	@Mock
	private MovimientoMapper movimientoMapper;

	private InventarioProperties inventarioProperties;
	private InventarioServiceImpl inventarioService;

	@BeforeEach
	void setUp() {
		inventarioProperties = new InventarioProperties();
		inventarioService = new InventarioServiceImpl(productoRepository, movimientoRepository, productoMapper,
				movimientoMapper, inventarioProperties);
	}

	@Test
	@DisplayName("Permite entrada cuando la cantidad respeta el límite configurado por defecto (1000)")
	void registrarEntradaPermitida() {
		inventarioProperties.setMaxUnidadesPorMovimiento(1000);
		Producto producto = new Producto("P-100", "Laptop Dell", "Core i7", new BigDecimal("3500.00"), 5);

		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
		when(movimientoRepository.save(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));
		when(movimientoMapper.toResponse(any(MovimientoInventario.class)))
				.thenReturn(new MovimientoResponse(1L, 1L, "P-100", TipoMovimiento.ENTRADA, 500, 500, "Compra inicial", LocalDateTime.now()));

		MovimientoRequest request = new MovimientoRequest(1L, 500, "Compra inicial");
		MovimientoResponse response = inventarioService.registrarEntrada(request);

		assertNotNull(response);
		assertEquals(500, producto.getStockActual());
		verify(movimientoRepository).save(any(MovimientoInventario.class));
	}

	@Test
	@DisplayName("Rechaza entrada cuando la cantidad excede el límite del perfil (ej. 50 en dev o 1000 en base)")
	void registrarEntradaSuperaLimiteLanzaExcepcion() {
		// Simula perfil dev donde el límite configurado es 50
		inventarioProperties.setMaxUnidadesPorMovimiento(50);

		MovimientoRequest request = new MovimientoRequest(1L, 60, "Ingreso de lote masivo");

		ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
				() -> inventarioService.registrarEntrada(request));

		assertEquals("Un movimiento no puede superar 50 unidades", ex.getMessage());
		verify(movimientoRepository, never()).save(any());
	}

	@Test
	@DisplayName("Rechaza movimiento sobre producto inactivo")
	void rechazaMovimientoProductoInactivo() {
		inventarioProperties.setMaxUnidadesPorMovimiento(100);
		Producto producto = new Producto("P-101", "Mouse", null, new BigDecimal("50.00"), 2);
		producto.desactivar();

		when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));

		MovimientoRequest request = new MovimientoRequest(2L, 10, "Reposicion");

		ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
				() -> inventarioService.registrarEntrada(request));

		assertEquals("El producto P-101 está inactivo", ex.getMessage());
		verify(movimientoRepository, never()).save(any());
	}
}
