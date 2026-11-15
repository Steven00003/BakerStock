package com.bakerstock.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bakerstock.dominio.Insumo;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

	boolean existsByNombreIgnoreCase(String nombre);

	Optional<Insumo> findByNombreIgnoreCase(String nombre);

}
