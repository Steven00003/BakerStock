package com.bakerstock.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public class InsumoForm {

	@NotBlank(message = "DATO_INVALIDO")
	private String nombre;

	/**
	 * RN-01: la unidad base solo puede ser g, ml o unidad.
	 */
	@NotBlank(message = "DATO_INVALIDO")
	@Pattern(regexp = "G|ML|UNIDAD", message = "Use g, ml o unidad (DATO_INVALIDO)")
	private String unidadBase;

	@NotBlank(message = "DATO_INVALIDO")
	@Pattern(regexp = "HARINAS|LACTEOS_Y_HUEVOS|GRASAS|LEVADURAS_Y_AGENTES|AZUCARES|RELLENOS_Y_FRUTAS|EMPAQUES|OTROS",
			message = "DATO_INVALIDO")
	private String categoria;

	@NotNull(message = "DATO_INVALIDO")
	@PositiveOrZero(message = "DATO_INVALIDO")
	private BigDecimal stockMinimo;

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getUnidadBase() {
		return unidadBase;
	}

	public void setUnidadBase(String unidadBase) {
		this.unidadBase = unidadBase;
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public BigDecimal getStockMinimo() {
		return stockMinimo;
	}

	public void setStockMinimo(BigDecimal stockMinimo) {
		this.stockMinimo = stockMinimo;
	}

}
