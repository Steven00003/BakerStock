package com.bakerstock.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bakerstock.dominio.Categoria;
import com.bakerstock.dominio.Insumo;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

	boolean existsByNombreIgnoreCase(String nombre);

	Optional<Insumo> findByNombreIgnoreCase(String nombre);

	/**
	 * Carga el insumo con todos sus lotes en una sola consulta (sin N+1). El
	 * orden FEFO lo impone el @OrderBy de la colección.
	 */
	@EntityGraph(attributePaths = "lotes")
	Optional<Insumo> findWithLotesById(Long id);

	/**
	 * Búsqueda de insumos (HU-07): coincidencia parcial sin distinguir
	 * mayúsculas en el nombre, filtro exacto por categoría y orden por nombre.
	 * Sin texto ni categoría lista todos.
	 */
	@Query("""
			select i from Insumo i
			where (:texto is null or lower(i.nombre) like lower(concat('%', :texto, '%')))
			  and (:categoria is null or i.categoria = :categoria)
			order by i.nombre
			""")
	List<Insumo> buscar(@Param("texto") String texto, @Param("categoria") Categoria categoria);

}
