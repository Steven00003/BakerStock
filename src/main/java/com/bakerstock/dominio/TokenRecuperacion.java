package com.bakerstock.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Token de un solo uso para recuperar la contraseña (D-01). Solo se guarda el
 * hash SHA-256 del token; el token en claro nunca se persiste.
 */
@Entity
@Table(name = "token_recuperacion")
public class TokenRecuperacion {

	private static final int MINUTOS_VIGENCIA = 30;
	private static final SecureRandom ALEATORIO = new SecureRandom();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(name = "token_hash", nullable = false, unique = true)
	private String tokenHash;

	@Column(name = "expira_en", nullable = false, updatable = false)
	private LocalDateTime expiraEn;

	@Column(nullable = false)
	private boolean usado;

	public static TokenRecuperacion crear(Usuario usuario, String tokenHash, LocalDateTime ahora) {
		TokenRecuperacion token = new TokenRecuperacion();
		token.usuario = usuario;
		token.tokenHash = tokenHash;
		token.expiraEn = ahora.plusMinutes(MINUTOS_VIGENCIA);
		return token;
	}

	public static String generarTokenClaro() {
		byte[] bytes = new byte[32];
		ALEATORIO.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static String hashDe(String tokenClaro) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(tokenClaro.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexadecimal = new StringBuilder(64);
			for (byte b : hash) {
				hexadecimal.append(String.format("%02x", b));
			}
			return hexadecimal.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no está disponible en la JVM", e);
		}
	}

	public boolean esUsableEn(LocalDateTime ahora) {
		return !usado && expiraEn.isAfter(ahora);
	}

	public void marcarUsado() {
		this.usado = true;
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public LocalDateTime getExpiraEn() {
		return expiraEn;
	}

	public boolean isUsado() {
		return usado;
	}

}
