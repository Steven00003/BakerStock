package com.bakerstock.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Insumo de la panadería (HU-05). Todas las cantidades están en la unidad base
 * del insumo (RN-01).
 */
@Entity
@Table(name = "insumo")
public class Insumo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "unidad_base", nullable = false, length = 40)
	private UnidadBase unidadBase;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 40)
	private Categoria categoria;

	@Column(name = "stock_minimo", nullable = false, precision = 14, scale = 3)
	private BigDecimal stockMinimo;

	@Column(nullable = false)
	private boolean activo;

	@OneToMany(mappedBy = "insumo")
	private List<Lote> lotes = new ArrayList<>();

	public BigDecimal stockTotal() {
		return lotes.stream()
				.map(Lote::getCantidadActual)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal stockUtilizable() {
		LocalDate hoy = LocalDate.now();
		return lotes.stream()
				.filter(lote -> !lote.estaVencido(hoy))
				.map(Lote::getCantidadActual)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public UnidadBase getUnidadBase() {
		return unidadBase;
	}

	public void setUnidadBase(UnidadBase unidadBase) {
		this.unidadBase = unidadBase;
	}

	public Categoria getCategoria() {
		return categoria;
	}

	public void setCategoria(Categoria categoria) {
		this.categoria = categoria;
	}

	public BigDecimal getStockMinimo() {
		return stockMinimo;
	}

	public void setStockMinimo(BigDecimal stockMinimo) {
		this.stockMinimo = stockMinimo;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	public List<Lote> getLotes() {
		return lotes;
	}

}
