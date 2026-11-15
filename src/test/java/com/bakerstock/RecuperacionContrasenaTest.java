package com.bakerstock;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.bakerstock.dominio.AccionAuditoria;
import com.bakerstock.dominio.EventoAuditoria;
import com.bakerstock.dominio.ResultadoAuditoria;
import com.bakerstock.dominio.Rol;
import com.bakerstock.dominio.TokenRecuperacion;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.EventoAuditoriaRepository;
import com.bakerstock.repositorio.TokenRecuperacionRepository;
import com.bakerstock.repositorio.UsuarioRepository;
import com.bakerstock.web.RecuperacionController;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecuperacionContrasenaTest {

	private static final String CLAVE_VIEJA = "ClaveVieja1*";
	private static final String MENSAJE_NEUTRO = "Si el correo está registrado, se generó un enlace de recuperación en el log de la aplicación.";
	private static final String MENSAJE_TOKEN_INVALIDO = "El enlace de recuperación no es válido o ya expiró.";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private TokenRecuperacionRepository tokens;

	@Autowired
	private EventoAuditoriaRepository auditoria;

	@Autowired
	private PasswordEncoder encoder;

	private ListAppender<ILoggingEvent> capturaLog;

	@BeforeEach
	void prepararCapturaLog() {
		Logger logger = (Logger) LoggerFactory.getLogger(RecuperacionController.class);
		capturaLog = new ListAppender<>();
		capturaLog.start();
		logger.addAppender(capturaLog);
	}

	@AfterEach
	void liberarCapturaLog() {
		Logger logger = (Logger) LoggerFactory.getLogger(RecuperacionController.class);
		logger.detachAppender(capturaLog);
	}

	private String crearUsuario(Rol rol, boolean activo) {
		Usuario usuario = new Usuario();
		usuario.setNombre(rol.name());
		usuario.setCorreo(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@bakerstock.test");
		usuario.setContrasenaHash(encoder.encode(CLAVE_VIEJA));
		usuario.setRol(rol);
		usuario.setActivo(activo);
		return usuarios.save(usuario).getCorreo();
	}

	private String solicitarToken(String correo) throws Exception {
		mockMvc.perform(post("/recuperar").with(csrf()).param("correo", correo))
				.andExpect(status().isOk());
		return capturaLog.list.stream()
				.map(ILoggingEvent::getFormattedMessage)
				.filter(m -> m.contains("Enlace de recuperación para " + correo))
				.map(m -> m.substring(m.indexOf("token=") + 6))
				.findFirst()
				.orElseThrow();
	}

	@Test
	@DisplayName("AC-018.1 una cuenta activa solicita recuperación: mensaje neutro, token que vence a los 30 minutos y enlace en el log")
	void cuentaActivaGeneraToken() throws Exception {
		String correo = crearUsuario(Rol.CAJERO, true);

		mockMvc.perform(post("/recuperar").with(csrf()).param("correo", correo))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(MENSAJE_NEUTRO)));

		String mensaje = capturaLog.list.stream()
				.map(ILoggingEvent::getFormattedMessage)
				.filter(m -> m.contains("Enlace de recuperación para " + correo + ": "))
				.findFirst()
				.orElseThrow();
		assertThat(mensaje).startsWith("Enlace de recuperación para " + correo + ": ");

		TokenRecuperacion token = tokens.findByTokenHash(TokenRecuperacion.hashDe(tokenClaroDelLog(correo))).orElseThrow();
		assertThat(token.isUsado()).isFalse();
		assertThat(token.getUsuario().getCorreo()).isEqualTo(correo);
		assertThat(token.getExpiraEn()).isCloseTo(LocalDateTime.now().plusMinutes(30), within(2, ChronoUnit.MINUTES));

		assertThat(auditoria.findByUsuarioCorreoAndAccion(correo, AccionAuditoria.RECUPERACION_SOLICITADA)).isNotEmpty();
	}

	private String tokenClaroDelLog(String correo) {
		return capturaLog.list.stream()
				.map(ILoggingEvent::getFormattedMessage)
				.filter(m -> m.contains("Enlace de recuperación para " + correo))
				.map(m -> m.substring(m.indexOf("token=") + 6))
				.findFirst()
				.orElseThrow();
	}

	@Test
	@DisplayName("AC-018.2 un correo que no existe recibe el mismo mensaje neutro y no se crea token")
	void correoInexistenteSinToken() throws Exception {
		String correo = "nadie-" + UUID.randomUUID() + "@bakerstock.test";
		long tokensAntes = tokens.count();

		mockMvc.perform(post("/recuperar").with(csrf()).param("correo", correo))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(MENSAJE_NEUTRO)));

		assertThat(tokens.count()).isEqualTo(tokensAntes);
		assertThat(capturaLog.list.stream()
				.map(ILoggingEvent::getFormattedMessage)
				.noneMatch(m -> m.contains("Enlace de recuperación para " + correo))).isTrue();
	}

	@Test
	@DisplayName("AC-018.3 un token ya usado es rechazado y la contraseña no vuelve a cambiar")
	void tokenUsadoRechazado() throws Exception {
		String correo = crearUsuario(Rol.PANADERO, true);
		String tokenClaro = solicitarToken(correo);
		Long id = usuarios.findByCorreoIgnoreCase(correo).orElseThrow().getId();

		restablecerCon(tokenClaro, "ClaveNueva1");
		assertThat(encoder.matches("ClaveNueva1", usuarios.findById(id).orElseThrow().getContrasenaHash())).isTrue();

		mockMvc.perform(get("/restablecer").param("token", tokenClaro))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(MENSAJE_TOKEN_INVALIDO)));

		restablecerCon(tokenClaro, "OtraClave22");
		assertThat(encoder.matches("OtraClave22", usuarios.findById(id).orElseThrow().getContrasenaHash())).isFalse();
		assertThat(encoder.matches("ClaveNueva1", usuarios.findById(id).orElseThrow().getContrasenaHash())).isTrue();
	}

	@Test
	@DisplayName("AC-018.3 un token vencido es rechazado y la contraseña no cambia")
	void tokenVencidoRechazado() throws Exception {
		String correo = crearUsuario(Rol.COMPRADOR, true);
		Long id = usuarios.findByCorreoIgnoreCase(correo).orElseThrow().getId();
		String tokenClaro = TokenRecuperacion.generarTokenClaro();
		tokens.save(TokenRecuperacion.crear(usuarios.findByCorreoIgnoreCase(correo).orElseThrow(),
				TokenRecuperacion.hashDe(tokenClaro), LocalDateTime.now().minusMinutes(31)));

		mockMvc.perform(get("/restablecer").param("token", tokenClaro))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(MENSAJE_TOKEN_INVALIDO)));

		restablecerCon(tokenClaro, "ClaveNueva1");
		assertThat(encoder.matches("ClaveNueva1", usuarios.findById(id).orElseThrow().getContrasenaHash())).isFalse();
		assertThat(encoder.matches(CLAVE_VIEJA, usuarios.findById(id).orElseThrow().getContrasenaHash())).isTrue();
	}

	@Test
	@DisplayName("AC-018.4 un token válido con contraseña nueva válida: cambia con BCrypt, token invalidado y evento registrado")
	void restablecimientoValido() throws Exception {
		String correo = crearUsuario(Rol.CAJERO, true);
		Long id = usuarios.findByCorreoIgnoreCase(correo).orElseThrow().getId();
		String tokenClaro = solicitarToken(correo);

		mockMvc.perform(post("/restablecer").with(csrf())
						.param("token", tokenClaro)
						.param("contrasena", "ClaveNueva1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?restablecido"));

		Usuario usuario = usuarios.findById(id).orElseThrow();
		assertThat(encoder.matches("ClaveNueva1", usuario.getContrasenaHash())).isTrue();
		assertThat(encoder.matches(CLAVE_VIEJA, usuario.getContrasenaHash())).isFalse();

		TokenRecuperacion token = tokens.findByTokenHash(TokenRecuperacion.hashDe(tokenClaro)).orElseThrow();
		assertThat(token.isUsado()).isTrue();

		EventoAuditoria evento = auditoria.findByUsuarioCorreoAndAccion(correo, AccionAuditoria.CONTRASENA_RESTABLECIDA)
				.stream()
				.findFirst()
				.orElseThrow();
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoria.OK);
	}

	@Test
	@DisplayName("NFR-BAK-004 la nueva contraseña debe tener mínimo 8 caracteres con letra y número: DATO_INVALIDO y sin gastar el token")
	void contrasenaNuevaInvalida() throws Exception {
		String correo = crearUsuario(Rol.ALMACENISTA, true);
		Long id = usuarios.findByCorreoIgnoreCase(correo).orElseThrow().getId();
		String tokenClaro = solicitarToken(correo);

		mockMvc.perform(post("/restablecer").with(csrf())
						.param("token", tokenClaro)
						.param("contrasena", "abc"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("DATO_INVALIDO")));

		assertThat(encoder.matches("abc", usuarios.findById(id).orElseThrow().getContrasenaHash())).isFalse();
		assertThat(encoder.matches(CLAVE_VIEJA, usuarios.findById(id).orElseThrow().getContrasenaHash())).isTrue();
		assertThat(tokens.findByTokenHash(TokenRecuperacion.hashDe(tokenClaro)).orElseThrow().isUsado()).isFalse();
	}

	@Test
	@DisplayName("REQ-027 token inexistente: registra RECUPERACION_RECHAZADA con motivo INVALIDO y sin el token en el detalle")
	void rechazoTokenInvalido() throws Exception {
		String tokenClaro = TokenRecuperacion.generarTokenClaro();

		mockMvc.perform(get("/restablecer").param("token", tokenClaro))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(MENSAJE_TOKEN_INVALIDO)));

		EventoAuditoria evento = auditoria.findByUsuarioCorreoAndAccion("anonimo", AccionAuditoria.RECUPERACION_RECHAZADA)
				.stream()
				.findFirst()
				.orElseThrow();
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoria.RECHAZADO);
		assertThat(evento.getDetalle()).isEqualTo("INVALIDO");
		assertThat(evento.getDetalle()).doesNotContain(tokenClaro);
	}

	@Test
	@DisplayName("REQ-027 token ya usado: registra RECUPERACION_RECHAZADA con motivo USADO")
	void rechazoTokenUsado() throws Exception {
		String correo = crearUsuario(Rol.PANADERO, true);
		String tokenClaro = solicitarToken(correo);
		restablecerCon(tokenClaro, "ClaveNueva1");

		mockMvc.perform(post("/restablecer").with(csrf())
						.param("token", tokenClaro)
						.param("contrasena", "OtraClave22"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(MENSAJE_TOKEN_INVALIDO)));

		EventoAuditoria evento = auditoria.findByUsuarioCorreoAndAccion(correo, AccionAuditoria.RECUPERACION_RECHAZADA)
				.stream()
				.findFirst()
				.orElseThrow();
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoria.RECHAZADO);
		assertThat(evento.getDetalle()).isEqualTo("USADO");
		assertThat(evento.getDetalle()).doesNotContain(tokenClaro);
	}

	@Test
	@DisplayName("REQ-027 token vencido: registra RECUPERACION_RECHAZADA con motivo VENCIDO")
	void rechazoTokenVencido() throws Exception {
		String correo = crearUsuario(Rol.COMPRADOR, true);
		String tokenClaro = TokenRecuperacion.generarTokenClaro();
		tokens.save(TokenRecuperacion.crear(usuarios.findByCorreoIgnoreCase(correo).orElseThrow(),
				TokenRecuperacion.hashDe(tokenClaro), LocalDateTime.now().minusMinutes(31)));

		mockMvc.perform(post("/restablecer").with(csrf())
						.param("token", tokenClaro)
						.param("contrasena", "ClaveNueva1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(MENSAJE_TOKEN_INVALIDO)));

		EventoAuditoria evento = auditoria.findByUsuarioCorreoAndAccion(correo, AccionAuditoria.RECUPERACION_RECHAZADA)
				.stream()
				.findFirst()
				.orElseThrow();
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoria.RECHAZADO);
		assertThat(evento.getDetalle()).isEqualTo("VENCIDO");
		assertThat(evento.getDetalle()).doesNotContain(tokenClaro);
	}

	private void restablecerCon(String tokenClaro, String contrasena) throws Exception {
		mockMvc.perform(post("/restablecer").with(csrf())
				.param("token", tokenClaro)
				.param("contrasena", contrasena));
	}

}
