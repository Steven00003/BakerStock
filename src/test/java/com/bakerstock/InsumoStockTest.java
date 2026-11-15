package com.bakerstock;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bakerstock.dominio.Categoria;
import com.bakerstock.dominio.Insumo;
import com.bakerstock.dominio.Lote;
import com.bakerstock.dominio.UnidadBase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de dominio de HU-08 sin Spring: cálculo de stock y FEFO.
 */
class InsumoStockTest {

	private final LocalDate hoy = LocalDate.now();

	private Insumo insumo(String nombre) {
		Insumo insumo = new Insumo();
		insumo.setNombre(nombre);
		insumo.setUnidadBase(UnidadBase.G);
		insumo.setCategoria(Categoria.HARINAS);
		insumo.setStockMinimo(new BigDecimal("1000"));
		insumo.setActivo(true);
		return insumo;
	}

	private Lote nuevoLote(BigDecimal cantidad, LocalDate vencimiento) {
		Lote lote = new Lote();
		lote.setNumeroLote("L-" + vencimiento);
		lote.setCantidadInicial(cantidad);
		lote.setCantidadActual(cantidad);
		lote.setCostoUnitario(BigDecimal.ONE);
		lote.setFechaRecepcion(vencimiento.minusDays(30));
		lote.setFechaVencimiento(vencimiento);
		return lote;
	}

	@Test
	@DisplayName("AC-026.1 stockTotal suma cantidadActual de todos los lotes: 1000 + 1500 = 2500")
	void stockTotalSumaTodosLosLotes() {
		Insumo harina = insumo("Harina de trigo");
		Lote loteA = nuevoLote(new BigDecimal("1000"), hoy.plusDays(40));
		Lote loteB = nuevoLote(new BigDecimal("1500"), hoy.plusDays(30));
		harina.getLotes().add(loteA);
		harina.getLotes().add(loteB);

		assertThat(harina.stockTotal()).isEqualByComparingTo("2500");
	}

	@Test
	@DisplayName("AC-026.2 stockUtilizable(hoy) suma solo los no vencidos: vencido 500 fuera, utilizable 1000 (vence hoy); estaVencido es verdadero solo si el vencimiento es anterior a hoy")
	void stockUtilizableExcluyeVencidos() {
		Lote vencido = nuevoLote(new BigDecimal("500"), hoy.minusDays(1));
		Lote venceHoy = nuevoLote(new BigDecimal("1000"), hoy);
		Lote venceManana = nuevoLote(new BigDecimal("1"), hoy.plusDays(1));
		assertThat(vencido.estaVencido(hoy)).isTrue();
		assertThat(venceHoy.estaVencido(hoy)).isFalse();
		assertThat(venceManana.estaVencido(hoy)).isFalse();

		Insumo harina = insumo("Harina de trigo");
		harina.getLotes().add(vencido);
		harina.getLotes().add(venceHoy);

		assertThat(harina.stockTotal()).isEqualByComparingTo("1500");
		assertThat(harina.stockUtilizable(hoy)).isEqualByComparingTo("1000");
	}

	@Test
	@DisplayName("AC-026.3 un insumo sin lotes tiene stock total y utilizable 0")
	void stockCeroSinLotes() {
		Insumo mermelada = insumo("Mermelada de fresa");

		assertThat(mermelada.stockTotal()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(mermelada.stockUtilizable(hoy)).isEqualByComparingTo(BigDecimal.ZERO);
	}

}
