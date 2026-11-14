package com.bakerstock;

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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioAdminTest {

	private static final String CLAVE = "ClaveSegura1*";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private EventoAuditoriaRepository auditoria;

	@Autowired
	private PasswordEncoder encoder;

	private String crearUsuario(Rol rol, boolean activo) {
		Usuario usuario = new Usuario();
		usuario.setNombre(rol.name());
		usuario.setCorreo(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@bakerstock.test");
		usuario.setContrasenaHash(encoder.encode(CLAVE));
		usuario.setRol(rol);
		usuario.setActivo(activo);
		return usuarios.save(usuario).getCorreo();
	}

	private MockHttpSession iniciarSesion(String correo) throws Exception {
		MvcResult login = mockMvc.perform(formLogin().user(correo).password(CLAVE))
				.andExpect(redirectedUrl("/"))
				.andReturn();
		return (MockHttpSession) login.getRequest().getSession(false);
	}

	private Long idDe(String correo) {
		return usuarios.findByCorreoIgnoreCase(correo).orElseThrow().getId();
	}

	@Test
	@DisplayName("AC-019.1 el Administrador crea un usuario con rol PANADERO y contraseña BCrypt")
	void crearUsuarioPanadero() throws Exception {
		String correoAdmin = crearUsuario(Rol.ADMINISTRADOR, true);
		MockHttpSession sesion = iniciarSesion(correoAdmin);
		String correo = "panadero-" + UUID.randomUUID() + "@bakerstock.test";
		crearDesde(sesion, correo, "Panadero de Prueba", "PANADERO", "Clave1234");

		Usuario creado = usuarios.findByCorreoIgnoreCase(correo).orElseThrow();
		assertThat(creado.getRol()).isEqualTo(Rol.PANADERO);
		assertThat(creado.isActivo()).isTrue();
		assertThat(creado.getContrasenaHash()).matches("^\\$2[aby]\\$\\S+$");
		assertThat(encoder.matches("Clave1234", creado.getContrasenaHash())).isTrue();

		EventoAuditoria evento = auditoria.findByUsuarioCorreoAndAccion(correoAdmin, AccionAuditoria.USUARIO_CREADO)
				.stream()
				.findFirst()
				.orElseThrow();
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoria.OK);
		assertThat(evento.getDetalle()).isEqualTo(correo);
	}

	@Test
	@DisplayName("AC-019.2 correo duplicado: rechaza con DATO_DUPLICADO y no crea el segundo usuario")
	void correoDuplicado() throws Exception {
		String correoAdmin = crearUsuario(Rol.ADMINISTRADOR, true);
		MockHttpSession sesion = iniciarSesion(correoAdmin);
		String correo = "duplicado-" + UUID.randomUUID() + "@bakerstock.test";
		crearDesde(sesion, correo, "Comprador de Prueba", "COMPRADOR", "Clave1234");
		long totalAntes = usuarios.count();

		mockMvc.perform(post("/admin/usuarios/nuevo").session(sesion).with(csrf())
						.param("nombre", "Otro Comprador")
						.param("correo", correo.toUpperCase())
						.param("rol", "COMPRADOR")
						.param("contrasena", "Clave5678"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("DATO_DUPLICADO")));

		assertThat(usuarios.count()).isEqualTo(totalAntes);
	}

	@Test
	@DisplayName("AC-N004.1 contraseña «abc»: rechaza con DATO_INVALIDO y no crea el usuario")
	void contrasenaInvalida() throws Exception {
		MockHttpSession sesion = iniciarSesion(crearUsuario(Rol.ADMINISTRADOR, true));
		String correo = "abc-" + UUID.randomUUID() + "@bakerstock.test";

		mockMvc.perform(post("/admin/usuarios/nuevo").session(sesion).with(csrf())
						.param("nombre", "Cajero de Prueba")
						.param("correo", correo)
						.param("rol", "CAJERO")
						.param("contrasena", "abc"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("DATO_INVALIDO")));

		assertThat(usuarios.findByCorreoIgnoreCase(correo)).isEmpty();
	}

	@Test
	@DisplayName("AC-020.1 desactivar una cuenta con historial: no puede iniciar sesión y el historial permanece")
	void desactivarConservaHistorial() throws Exception {
		String correoAdmin = crearUsuario(Rol.ADMINISTRADOR, true);
		MockHttpSession sesionAdmin = iniciarSesion(correoAdmin);
		String correo = crearUsuario(Rol.CAJERO, true);
		Long id = idDe(correo);

		mockMvc.perform(formLogin().user(correo).password(CLAVE))
				.andExpect(redirectedUrl("/"));
		assertThat(auditoria.findByUsuarioCorreoAndAccion(correo, AccionAuditoria.LOGIN_OK)).isNotEmpty();

		mockMvc.perform(post("/admin/usuarios/" + id + "/desactivar").session(sesionAdmin).with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/usuarios"));

		assertThat(usuarios.findById(id).orElseThrow().isActivo()).isFalse();
		mockMvc.perform(formLogin().user(correo).password(CLAVE))
				.andExpect(unauthenticated())
				.andExpect(redirectedUrl("/login?error"));

		assertThat(auditoria.findByUsuarioCorreoAndAccion(correo, AccionAuditoria.LOGIN_OK)).isNotEmpty();
		assertThat(usuarios.findById(id).orElseThrow().getCorreo()).isEqualTo(correo);
	}

	@Test
	@DisplayName("REQ-020 reactivar una cuenta desactivada: vuelve a iniciar sesión y registra USUARIO_ACTIVADO")
	void reactivarCuenta() throws Exception {
		String correoAdmin = crearUsuario(Rol.ADMINISTRADOR, true);
		MockHttpSession sesionAdmin = iniciarSesion(correoAdmin);
		String correo = crearUsuario(Rol.PANADERO, true);
		Long id = idDe(correo);

		mockMvc.perform(post("/admin/usuarios/" + id + "/desactivar").session(sesionAdmin).with(csrf()))
				.andExpect(redirectedUrl("/admin/usuarios"));
		mockMvc.perform(post("/admin/usuarios/" + id + "/activar").session(sesionAdmin).with(csrf()))
				.andExpect(redirectedUrl("/admin/usuarios"));

		assertThat(usuarios.findById(id).orElseThrow().isActivo()).isTrue();
		mockMvc.perform(formLogin().user(correo).password(CLAVE))
				.andExpect(redirectedUrl("/"));
		assertThat(auditoria.findByUsuarioCorreoAndAccion(correoAdmin, AccionAuditoria.USUARIO_ACTIVADO)
				.stream()
				.filter(e -> correo.equals(e.getDetalle()))
				.findFirst()
				.orElseThrow()
				.getResultado()).isEqualTo(ResultadoAuditoria.OK);
	}

	@Test
	@DisplayName("AC-028.1 el Administrador no puede desactivar su propia cuenta y esta sigue activa")
	void adminNoSeDesactiva() throws Exception {
		String correoAdmin = crearUsuario(Rol.ADMINISTRADOR, true);
		MockHttpSession sesion = iniciarSesion(correoAdmin);
		Long id = idDe(correoAdmin);

		mockMvc.perform(post("/admin/usuarios/" + id + "/desactivar").session(sesion).with(csrf()))
				.andExpect(status().is3xxRedirection());

		assertThat(usuarios.findById(id).orElseThrow().isActivo()).isTrue();
		EventoAuditoria evento = auditoria.findByUsuarioCorreoAndAccion(correoAdmin, AccionAuditoria.USUARIO_DESACTIVADO)
				.stream()
				.findFirst()
				.orElseThrow();
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoria.RECHAZADO);
	}

	private void crearDesde(MockHttpSession sesion, String correo, String nombre, String rol, String contrasena)
			throws Exception {
		mockMvc.perform(post("/admin/usuarios/nuevo").session(sesion).with(csrf())
				.param("nombre", nombre)
				.param("correo", correo)
				.param("rol", rol)
				.param("contrasena", contrasena))
				.andExpect(status().is3xxRedirection());
	}

}
