package com.bakerstock.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bakerstock.dominio.AccionAuditoria;
import com.bakerstock.dominio.EventoAuditoria;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {

	List<EventoAuditoria> findByUsuarioCorreoAndAccion(String usuarioCorreo, AccionAuditoria accion);

}
