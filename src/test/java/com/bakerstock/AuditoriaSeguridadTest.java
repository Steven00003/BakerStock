package com.bakerstock;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.bakerstock.dominio.AccionAuditoria;
import com.bakerstock.dominio.EventoAuditoria;
import com.bakerstock.dominio.Rol;
import com.bakerstock.dominio.ResultadoAuditoria;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.EventoAuditoriaRepository;
import com.bakerstock.repositorio.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaSeguridadTest {

	private static final String CLAVE = "ClaveSegura1*";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private EventoAuditoriaRepository auditoria;

	@Autowired
	private PasswordEncoder encoder;

	private String crearUsuarioActivo(Rol rol) {
		Usuario usuario = new Usuario();
		usuario.setNombre(rol.name());
		usuario.setCorreo(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@bakerstock.test");
		usuario.setContrasenaHash(encoder.encode(CLAVE));
		usuario.setRol(rol);
		usuario.setActivo(true);
		return usuarios.save(usuario).getCorreo();
	}

	@Test
	@DisplayName("AC-027.1 un inicio de sesión exitoso guarda LOGIN_OK con usuario y fecha-hora")
	void loginOkSeRegistra() throws Exception {
		String correo = crearUsuarioActivo(Rol.ADMINISTRADOR);
		mockMvc.perform(formLogin().user(correo).password(CLAVE));
		EventoAuditoria evento = auditoria.findByUsuarioCorreoAndAccion(correo, AccionAuditoria.LOGIN_OK)
				.stream()
				.findFirst()
				.orElseThrow();
		assertThat(evento.getUsuarioCorreo()).isEqualTo(correo);
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoria.OK);
		assertThat(evento.getFechaHora()).isNotNull();
		assertThat(evento.getFechaHora()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
	}

	@Test
	@DisplayName("AC-027.1 un cierre de sesión guarda LOGOUT con usuario y fecha-hora")
	void logoutSeRegistra() throws Exception {
		String correo = crearUsuarioActivo(Rol.PANADERO);
		MvcResult login = mockMvc.perform(formLogin().user(correo).password(CLAVE)).andReturn();
		MockHttpSession sesion = (MockHttpSession) login.getRequest().getSession(false);
		mockMvc.perform(post("/logout").session(sesion).with(csrf()));
		EventoAuditoria evento = auditoria.findByUsuarioCorreoAndAccion(correo, AccionAuditoria.LOGOUT)
				.stream()
				.findFirst()
				.orElseThrow();
		assertThat(evento.getUsuarioCorreo()).isEqualTo(correo);
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoria.OK);
		assertThat(evento.getFechaHora()).isNotNull();
	}

}
