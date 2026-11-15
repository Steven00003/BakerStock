package com.bakerstock.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bakerstock.dominio.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

	boolean existsByNit(String nit);

	List<Proveedor> findAllByOrderByNombreAsc();

}
