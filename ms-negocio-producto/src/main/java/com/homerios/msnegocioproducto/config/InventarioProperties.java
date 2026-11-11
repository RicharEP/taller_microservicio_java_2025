package com.homerios.msnegocioproducto.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades de configuración tipadas para el módulo de inventario.
 * Se enlazan al prefijo "inventario" definido en el Config Server.
 * Al usar una clase JavaBean estándar con setters, Spring Cloud refresca
 * los valores en caliente automáticamente ante peticiones a /actuator/refresh.
 */
@Validated
@ConfigurationProperties(prefix = "inventario")
public class InventarioProperties {

	/**
	 * Máximo de unidades permitidas en un solo movimiento de inventario.
	 */
	@Min(value = 1, message = "El máximo de unidades por movimiento debe ser al menos 1")
	private int maxUnidadesPorMovimiento = 1000;

	/**
	 * Si es true, se registra una advertencia cuando el stock resultante queda por debajo del mínimo.
	 */
	private boolean alertaStockBajo = true;

	public int getMaxUnidadesPorMovimiento() {
		return maxUnidadesPorMovimiento;
	}

	public void setMaxUnidadesPorMovimiento(int maxUnidadesPorMovimiento) {
		this.maxUnidadesPorMovimiento = maxUnidadesPorMovimiento;
	}

	public boolean isAlertaStockBajo() {
		return alertaStockBajo;
	}

	public void setAlertaStockBajo(boolean alertaStockBajo) {
		this.alertaStockBajo = alertaStockBajo;
	}
}
