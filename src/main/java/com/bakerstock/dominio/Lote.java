package com.bakerstock.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Lote de un insumo, recibido en una compra (HU-07). En el Sprint 1 solo se
 * lee; la recepción de compras lo crea en el Sprint 2.
 */
@Entity
@Table(name = "lote")
public class Lote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "insumo_id", nullable = false)
	private Insumo insumo;

	@Column(name = "numero_lote", nullable = false)
	private String numeroLote;

	@Column(name = "cantidad_inicial", nullable = false, precision = 14, scale = 3)
	private BigDecimal cantidadInicial;

	@Column(name = "cantidad_actual", nullable = false, precision = 14, scale = 3)
	private BigDecimal cantidadActual;

	@Column(name = "costo_unitario", nullable = false, precision = 14, scale = 3)
	private BigDecimal costoUnitario;

	@Column(name = "fecha_recepcion", nullable = false)
	private LocalDate fechaRecepcion;

	@Column(name = "fecha_vencimiento", nullable = false)
	private LocalDate fechaVencimiento;

	public boolean estaVencido(LocalDate hoy) {
		return fechaVencimiento.isBefore(hoy);
	}

	public Long getId() {
		return id;
	}

	public Insumo getInsumo() {
		return insumo;
	}

	public void setInsumo(Insumo insumo) {
		this.insumo = insumo;
	}

	public String getNumeroLote() {
		return numeroLote;
	}

	public void setNumeroLote(String numeroLote) {
		this.numeroLote = numeroLote;
	}

	public BigDecimal getCantidadInicial() {
		return cantidadInicial;
	}

	public void setCantidadInicial(BigDecimal cantidadInicial) {
		this.cantidadInicial = cantidadInicial;
	}

	public BigDecimal getCantidadActual() {
		return cantidadActual;
	}

	public void setCantidadActual(BigDecimal cantidadActual) {
		this.cantidadActual = cantidadActual;
	}

	public BigDecimal getCostoUnitario() {
		return costoUnitario;
	}

	public void setCostoUnitario(BigDecimal costoUnitario) {
		this.costoUnitario = costoUnitario;
	}

	public LocalDate getFechaRecepcion() {
		return fechaRecepcion;
	}

	public void setFechaRecepcion(LocalDate fechaRecepcion) {
		this.fechaRecepcion = fechaRecepcion;
	}

	public LocalDate getFechaVencimiento() {
		return fechaVencimiento;
	}

	public void setFechaVencimiento(LocalDate fechaVencimiento) {
		this.fechaVencimiento = fechaVencimiento;
	}

}
