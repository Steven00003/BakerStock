package com.bakerstock.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bakerstock.dominio.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByCorreoIgnoreCase(String correo);

}
