package com.bakerstock.seguridad;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

import com.bakerstock.dominio.AccionAuditoria;
import com.bakerstock.dominio.EventoAuditoria;
import com.bakerstock.dominio.ResultadoAuditoria;
import com.bakerstock.repositorio.EventoAuditoriaRepository;

@Component
public class AuditoriaSeguridadListener {

	private final EventoAuditoriaRepository eventoAuditoriaRepository;

	public AuditoriaSeguridadListener(EventoAuditoriaRepository eventoAuditoriaRepository) {
		this.eventoAuditoriaRepository = eventoAuditoriaRepository;
	}

	@EventListener
	public void alIniciarSesion(AuthenticationSuccessEvent evento) {
		guardar(evento.getAuthentication().getName(), AccionAuditoria.LOGIN_OK, ResultadoAuditoria.OK);
	}

	@EventListener
	public void alFallarAutenticacion(AbstractAuthenticationFailureEvent evento) {
		guardar(evento.getAuthentication().getName(), AccionAuditoria.LOGIN_FALLIDO, ResultadoAuditoria.RECHAZADO);
	}

	@EventListener
	public void alCerrarSesion(LogoutSuccessEvent evento) {
		guardar(evento.getAuthentication().getName(), AccionAuditoria.LOGOUT, ResultadoAuditoria.OK);
	}

	private void guardar(String correo, AccionAuditoria accion, ResultadoAuditoria resultado) {
		eventoAuditoriaRepository.save(EventoAuditoria.registrar(correo, accion, resultado, null));
	}

}
