package com.bakerstock.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "evento_auditoria")
public class EventoAuditoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "fecha_hora", nullable = false, updatable = false)
	private LocalDateTime fechaHora;

	@Column(name = "usuario_correo", nullable = false, updatable = false)
	private String usuarioCorreo;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, updatable = false, length = 40)
	private AccionAuditoria accion;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, updatable = false, length = 40)
	private ResultadoAuditoria resultado;

	@Column(updatable = false)
	private String detalle;

	public static EventoAuditoria registrar(String usuarioCorreo, AccionAuditoria accion,
			ResultadoAuditoria resultado, String detalle) {
		EventoAuditoria evento = new EventoAuditoria();
		evento.usuarioCorreo = usuarioCorreo;
		evento.accion = accion;
		evento.resultado = resultado;
		evento.detalle = detalle;
		return evento;
	}

	public Long getId() {
		return id;
	}

	public LocalDateTime getFechaHora() {
		return fechaHora;
	}

	public String getUsuarioCorreo() {
		return usuarioCorreo;
	}

	public AccionAuditoria getAccion() {
		return accion;
	}

	public ResultadoAuditoria getResultado() {
		return resultado;
	}

	public String getDetalle() {
		return detalle;
	}

	@PrePersist
	void asignarFechaHora() {
		if (fechaHora == null) {
			fechaHora = LocalDateTime.now();
		}
	}

	@PreUpdate
	@PreRemove
	void prohibirModificacion() {
		throw new IllegalStateException("Los eventos de auditoría son inmutables (RN-06)");
	}

}
