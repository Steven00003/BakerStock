package com.bakerstock.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bakerstock.dominio.Lote;

public interface LoteRepository extends JpaRepository<Lote, Long> {

	boolean existsByInsumoId(Long insumoId);

}
