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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccesoPorRolTest {

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

	private MockHttpSession iniciarSesion(Rol rol) throws Exception {
		String correo = crearUsuarioActivo(rol);
		MvcResult login = mockMvc.perform(formLogin().user(correo).password(CLAVE))
				.andExpect(redirectedUrl("/"))
				.andReturn();
		return (MockHttpSession) login.getRequest().getSession(false);
	}

	@Test
	@DisplayName("AC-008.1 el Cajero intenta registrar un insumo: 403, página «Acceso denegado» y evento con usuario, fecha-hora y recurso")
	void cajeroNoPuedeRegistrarInsumos() throws Exception {
		String correo = crearUsuarioActivo(Rol.CAJERO);
		MvcResult login = mockMvc.perform(formLogin().user(correo).password(CLAVE))
				.andExpect(redirectedUrl("/"))
				.andReturn();
		MockHttpSession sesion = (MockHttpSession) login.getRequest().getSession(false);

		mockMvc.perform(post("/insumos/nuevo").session(sesion).with(csrf()))
				.andExpect(status().isForbidden())
				.andExpect(content().string(containsString("Acceso denegado")));

		EventoAuditoria evento = auditoria.findByUsuarioCorreoAndAccion(correo, AccionAuditoria.ACCESO_DENEGADO)
				.stream()
				.findFirst()
				.orElseThrow();
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoria.RECHAZADO);
		assertThat(evento.getFechaHora()).isNotNull();
		assertThat(evento.getFechaHora()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
		assertThat(evento.getDetalle()).contains("/insumos/nuevo");
	}

	@Test
	@DisplayName("AC-008.2 un visitante sin sesión que abre /insumos es redirigido a /login")
	void visitanteEsRedirigidoALogin() throws Exception {
		mockMvc.perform(get("/insumos"))
				.andExpect(unauthenticated())
				.andExpect(redirectedUrlPattern("**/login"));
	}

}
