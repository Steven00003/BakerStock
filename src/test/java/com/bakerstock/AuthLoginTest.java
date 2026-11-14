package com.bakerstock;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.bakerstock.dominio.AccionAuditoria;
import com.bakerstock.dominio.Rol;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.EventoAuditoriaRepository;
import com.bakerstock.repositorio.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLoginTest {

	private static final String CLAVE = "ClaveSegura1*";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private EventoAuditoriaRepository auditoria;

	@Autowired
	private org.springframework.security.crypto.password.PasswordEncoder encoder;

	private String crearUsuario(Rol rol, boolean activo) {
		Usuario usuario = new Usuario();
		usuario.setNombre(rol.name());
		usuario.setCorreo(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@bakerstock.test");
		usuario.setContrasenaHash(encoder.encode(CLAVE));
		usuario.setRol(rol);
		usuario.setActivo(activo);
		return usuarios.save(usuario).getCorreo();
	}

	@Test
	@DisplayName("AC-015.1 un Almacenista activo con contraseña válida inicia sesión y entra a /")
	void almacenistaValidoEntra() throws Exception {
		String correo = crearUsuario(Rol.ALMACENISTA, true);
		MvcResult resultado = mockMvc.perform(formLogin().user(correo).password(CLAVE))
				.andExpect(SecurityMockMvcResultMatchers.authenticated())
				.andExpect(redirectedUrl("/"))
				.andReturn();
		assertThat(resultado.getRequest().getSession(false)).isNotNull();
	}

	@Test
	@DisplayName("AC-016.1 contraseña incorrecta: mensaje único y evento LOGIN_FALLIDO")
	void contrasenaIncorrecta() throws Exception {
		String correo = crearUsuario(Rol.ALMACENISTA, true);
		mockMvc.perform(formLogin().user(correo).password("mala"))
				.andExpect(SecurityMockMvcResultMatchers.unauthenticated())
				.andExpect(redirectedUrl("/login?error"));
		mockMvc.perform(get("/login").param("error", ""))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Correo o contraseña incorrectos")));
		assertThat(auditoria.findByUsuarioCorreoAndAccion(correo, AccionAuditoria.LOGIN_FALLIDO)).isNotEmpty();
	}

	@Test
	@DisplayName("AC-016.2 cuenta desactivada con contraseña correcta: mismo mensaje")
	void cuentaDesactivada() throws Exception {
		String correo = crearUsuario(Rol.CAJERO, false);
		mockMvc.perform(formLogin().user(correo).password(CLAVE))
				.andExpect(SecurityMockMvcResultMatchers.unauthenticated())
				.andExpect(redirectedUrl("/login?error"));
		mockMvc.perform(get("/login").param("error", ""))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Correo o contraseña incorrectos")));
	}

	@Test
	@DisplayName("AC-016.3 correo inexistente: mismo mensaje y evento LOGIN_FALLIDO")
	void correoInexistente() throws Exception {
		String correo = "nadie-" + UUID.randomUUID() + "@bakerstock.test";
		mockMvc.perform(formLogin().user(correo).password("loquesea"))
				.andExpect(SecurityMockMvcResultMatchers.unauthenticated())
				.andExpect(redirectedUrl("/login?error"));
		mockMvc.perform(get("/login").param("error", ""))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Correo o contraseña incorrectos")));
		assertThat(auditoria.findByUsuarioCorreoAndAccion(correo, AccionAuditoria.LOGIN_FALLIDO)).isNotEmpty();
	}

}
