package com.bakerstock.seguridad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.bakerstock.dominio.AccionAuditoria;
import com.bakerstock.dominio.EventoAuditoria;
import com.bakerstock.dominio.ResultadoAuditoria;
import com.bakerstock.repositorio.EventoAuditoriaRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AccesoDenegadoHandler implements AccessDeniedHandler {

	private final EventoAuditoriaRepository eventoAuditoriaRepository;

	public AccesoDenegadoHandler(EventoAuditoriaRepository eventoAuditoriaRepository) {
		this.eventoAuditoriaRepository = eventoAuditoriaRepository;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accesoDenegado) throws IOException {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		String usuario = autenticacion == null ? "anonimo" : autenticacion.getName();
		eventoAuditoriaRepository.save(EventoAuditoria.registrar(usuario, AccionAuditoria.ACCESO_DENEGADO,
				ResultadoAuditoria.RECHAZADO, request.getRequestURI()));
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType("text/html;charset=UTF-8");
		response.getWriter().write("""
				<!DOCTYPE html>
				<html lang="es">
				<head><meta charset="UTF-8"><title>Acceso denegado — BakerStock</title></head>
				<body>
					<h1>Acceso denegado</h1>
					<p>No tienes permiso para realizar esta operación. El intento quedó registrado.</p>
					<a href="/">Volver al inicio</a>
				</body>
				</html>
				""");
		response.getWriter().flush();
	}

}
