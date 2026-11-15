package com.bakerstock;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.bakerstock.dominio.Categoria;
import com.bakerstock.dominio.Insumo;
import com.bakerstock.dominio.Lote;
import com.bakerstock.dominio.UnidadBase;
import com.bakerstock.repositorio.InsumoRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class InsumoUnicidadTest {

	@Autowired
	private InsumoRepository insumos;

	@Autowired
	private TestEntityManager entityManager;

	private Insumo crear(String nombre) {
		Insumo insumo = new Insumo();
		insumo.setNombre(nombre);
		insumo.setUnidadBase(UnidadBase.G);
		insumo.setCategoria(Categoria.HARINAS);
		insumo.setStockMinimo(new BigDecimal("1000"));
		insumo.setActivo(true);
		return entityManager.persistAndFlush(insumo);
	}

	@Test
	@DisplayName("AC-022.3 la unicidad del nombre no distingue mayúsculas en el repositorio")
	void unicidadSinMayusculas() {
		crear("Harina de trigo");
		entityManager.clear();

		assertThat(insumos.existsByNombreIgnoreCase("harina de TRIGO")).isTrue();
		assertThat(insumos.existsByNombreIgnoreCase("Azúcar refinada")).isFalse();
	}

	@Test
	@DisplayName("REQ-022 un insumo recién creado tiene stock total y utilizable 0")
	void stockInicialCero() {
		Insumo insumo = crear("Levadura seca");

		assertThat(insumo.stockTotal()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(insumo.stockUtilizable()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	@DisplayName("REQ-022 los métodos de stock suman los lotes no vencidos del insumo")
	void stockConLotes() {
		Insumo insumo = crear("Mantequilla");
		LocalDate hoy = LocalDate.now();
		entityManager.persist(lote(insumo, new BigDecimal("800"), hoy.plusDays(30)));
		entityManager.persist(lote(insumo, new BigDecimal("300"), hoy.minusDays(1)));
		entityManager.persist(lote(insumo, new BigDecimal("500"), hoy.plusDays(10)));
		entityManager.flush();
		entityManager.clear();
		Insumo recargado = entityManager.find(Insumo.class, insumo.getId());

		assertThat(recargado.stockTotal()).isEqualByComparingTo(new BigDecimal("1600"));
		assertThat(recargado.stockUtilizable()).isEqualByComparingTo(new BigDecimal("1300"));
	}

	private Lote lote(Insumo insumo, BigDecimal cantidad, LocalDate vencimiento) {
		Lote lote = new Lote();
		lote.setInsumo(insumo);
		lote.setNumeroLote("L-1");
		lote.setCantidadInicial(cantidad);
		lote.setCantidadActual(cantidad);
		lote.setCostoUnitario(new BigDecimal("1"));
		lote.setFechaRecepcion(vencimiento.minusDays(30));
		lote.setFechaVencimiento(vencimiento);
		return lote;
	}

}
