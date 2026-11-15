package com.bakerstock;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.bakerstock.dominio.Categoria;
import com.bakerstock.dominio.Insumo;
import com.bakerstock.dominio.UnidadBase;
import com.bakerstock.repositorio.InsumoRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class InsumoBusquedaRepositorioTest {

	@Autowired
	private InsumoRepository insumos;

	@Autowired
	private TestEntityManager entityManager;

	private void persistir(String nombre, Categoria categoria) {
		Insumo insumo = new Insumo();
		insumo.setNombre(nombre);
		insumo.setUnidadBase(UnidadBase.G);
		insumo.setCategoria(categoria);
		insumo.setStockMinimo(new BigDecimal("1000"));
		insumo.setActivo(true);
		entityManager.persist(insumo);
	}

	@Test
	@DisplayName("AC-025.1 la búsqueda por «har» encuentra las harinas y no el azúcar, sin distinguir mayúsculas")
	void buscaPorTextoParcial() {
		persistir("Harina de trigo", Categoria.HARINAS);
		persistir("Harina integral", Categoria.HARINAS);
		persistir("Azúcar refinada", Categoria.AZUCARES);
		entityManager.flush();
		entityManager.clear();

		List<Insumo> resultados = insumos.buscar("har", null);

		assertThat(resultados).extracting(Insumo::getNombre)
				.containsExactlyInAnyOrder("Harina de trigo", "Harina integral");
		assertThat(insumos.buscar("HAR", null)).hasSameSizeAs(resultados);
	}

	@Test
	@DisplayName("AC-025.2 el filtro por LACTEOS_Y_HUEVOS sin texto muestra solo los de esa categoría")
	void filtraPorCategoria() {
		persistir("Leche entera", Categoria.LACTEOS_Y_HUEVOS);
		persistir("Huevo fresco", Categoria.LACTEOS_Y_HUEVOS);
		persistir("Azúcar refinada", Categoria.AZUCARES);
		entityManager.flush();
		entityManager.clear();

		List<Insumo> resultados = insumos.buscar(null, Categoria.LACTEOS_Y_HUEVOS);

		assertThat(resultados).extracting(Insumo::getNombre)
				.containsExactlyInAnyOrder("Leche entera", "Huevo fresco");
	}

	@Test
	@DisplayName("REQ-025 sin texto ni categoría lista todos los insumos ordenados por nombre")
	void listaTodosOrdenadosPorNombre() {
		persistir("Mantequilla", Categoria.GRASAS);
		persistir("Azúcar refinada", Categoria.AZUCARES);
		persistir("Bolsa de papel", Categoria.EMPAQUES);
		entityManager.flush();
		entityManager.clear();

		List<Insumo> resultados = insumos.buscar(null, null);

		assertThat(resultados).extracting(Insumo::getNombre)
				.containsExactly("Azúcar refinada", "Bolsa de papel", "Mantequilla");
	}

	@Test
	@DisplayName("AC-025.3 la búsqueda por «zzz» no devuelve resultados")
	void buscaSinResultados() {
		persistir("Harina de trigo", Categoria.HARINAS);
		entityManager.flush();
		entityManager.clear();

		assertThat(insumos.buscar("zzz", null)).isEmpty();
	}

}
