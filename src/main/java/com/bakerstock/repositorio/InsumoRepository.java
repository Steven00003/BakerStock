package com.bakerstock.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
