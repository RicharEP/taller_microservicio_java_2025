package com.homerios.msnegocioproducto.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Registro inmutable del kardex: una entrada o salida de un producto. */
@Entity
@Table(name = "movimiento_inventario", schema = "sys")
public class MovimientoInventario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_movimiento")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_producto", nullable = false, updatable = false)
	private Producto producto;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 10)
	private TipoMovimiento tipo;

	@Column(nullable = false, updatable = false)
	private int cantidad;

	@Column(name = "stock_resultante", nullable = false, updatable = false)
	private int stockResultante;

	@Column(length = 255, updatable = false)
	private String motivo;

	@Column(name = "fecha_registro", nullable = false, updatable = false)
	private LocalDateTime fechaRegistro;

	protected MovimientoInventario() {
		// requerido por JPA
	}

	public MovimientoInventario(Producto producto, TipoMovimiento tipo, int cantidad, String motivo) {
		this.producto = producto;
		this.tipo = tipo;
		this.cantidad = cantidad;
		this.stockResultante = producto.getStockActual();
		this.motivo = motivo;
		this.fechaRegistro = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Producto getProducto() {
		return producto;
	}

	public TipoMovimiento getTipo() {
		return tipo;
	}

	public int getCantidad() {
		return cantidad;
	}

	public int getStockResultante() {
		return stockResultante;
	}

	public String getMotivo() {
		return motivo;
	}

	public LocalDateTime getFechaRegistro() {
		return fechaRegistro;
	}
}
