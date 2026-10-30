package com.homerios.msnegocioproducto.model;

import java.math.BigDecimal;

import com.homerios.msnegocioproducto.exception.StockInsuficienteException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "producto", schema = "sys")
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_producto")
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String codigo;

	@Column(nullable = false, length = 150)
	private String nombre;

	@Column(length = 255)
	private String descripcion;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal precio;

	@Column(name = "stock_actual", nullable = false)
	private int stockActual;

	@Column(name = "stock_minimo", nullable = false)
	private int stockMinimo;

	@Column(nullable = false)
	private boolean activo = true;

	@Version
	private long version;

	protected Producto() {
		// requerido por JPA
	}

	public Producto(String codigo, String nombre, String descripcion, BigDecimal precio, int stockMinimo) {
		this.codigo = codigo;
		actualizarDatos(nombre, descripcion, precio, stockMinimo);
	}

	public void actualizarDatos(String nombre, String descripcion, BigDecimal precio, int stockMinimo) {
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.precio = precio;
		this.stockMinimo = stockMinimo;
	}

	public void incrementarStock(int cantidad) {
		this.stockActual += cantidad;
	}

	public void disminuirStock(int cantidad) {
		if (cantidad > stockActual) {
			throw new StockInsuficienteException(codigo, stockActual, cantidad);
		}
		this.stockActual -= cantidad;
	}

	public boolean tieneStockBajo() {
		return stockActual <= stockMinimo;
	}

	public void desactivar() {
		this.activo = false;
	}

	public Long getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public int getStockActual() {
		return stockActual;
	}

	public int getStockMinimo() {
		return stockMinimo;
	}

	public boolean isActivo() {
		return activo;
	}
}
