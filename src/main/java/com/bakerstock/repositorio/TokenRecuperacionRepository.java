package com.bakerstock.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bakerstock.dominio.TokenRecuperacion;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

	Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

}
