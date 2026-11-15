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
import com.bakerstock.dominio.ResultadoAuditoria;
import com.bakerstock.dominio.Rol;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.EventoAuditoriaRepository;
import com.bakerstock.repositorio.ProveedorRepository;
import com.bakerstock.repositorio.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProveedorRegistroTest {

	private static final String CLAVE = "ClaveSegura1*";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private ProveedorRepository proveedores;

	@Autowired
	private EventoAuditoriaRepository auditoria;

	@Autowired
	private PasswordEncoder encoder;

	private String crearUsuario(Rol rol) {
		Usuario usuario = new Usuario();
		usuario.setNombre(rol.name());
		usuario.setCorreo(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@bakerstock.test");
		usuario.setContrasenaHash(encoder.encode(CLAVE));
		usuario.setRol(rol);
		usuario.setActivo(true);
		return usuarios.save(usuario).getCorreo();
	}

	private MockHttpSession iniciarSesion(String correo) throws Exception {
		MvcResult login = mockMvc.perform(formLogin().user(correo).password(CLAVE))
				.andExpect(redirectedUrl("/"))
				.andReturn();
		return (MockHttpSession) login.getRequest().getSession(false);
	}

	private MockHttpSession iniciarSesion(Rol rol) throws Exception {
		return iniciarSesion(crearUsuario(rol));
	}

	@Test
	@DisplayName("AC-024.1 el Comprador registra «Distribuidora Ficticia S.A.S.» con NIT y teléfono: se guarda y se audita PROVEEDOR_CREADO")
	void altaCorrecta() throws Exception {
		String correoComprador = crearUsuario(Rol.COMPRADOR);
		MockHttpSession sesion = iniciarSesion(correoComprador);
		String nit = "90" + UUID.randomUUID().toString().substring(0, 6) + "-1";

		mockMvc.perform(get("/proveedores/nuevo").session(sesion))
				.andExpect(status().isOk());

		mockMvc.perform(post("/proveedores/nuevo").session(sesion).with(csrf())
						.param("nombre", "Distribuidora Ficticia S.A.S.")
						.param("nit", nit)
						.param("telefono", "3001234567")
						.param("correo", "")
						.param("direccion", ""))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/proveedores?creado"));

		assertThat(proveedores.existsByNit(nit)).isTrue();
		EventoAuditoria evento = auditoria.findByUsuarioCorreoAndAccion(correoComprador, AccionAuditoria.PROVEEDOR_CREADO)
				.stream()
				.findFirst()
				.orElseThrow();
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoria.OK);
		assertThat(evento.getDetalle()).isEqualTo("Distribuidora Ficticia S.A.S.");
	}

	@Test
	@DisplayName("AC-024.2 el Comprador registra otro proveedor con el mismo NIT: rechaza con DATO_DUPLICADO y no guarda otro")
	void nitRepetido() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.COMPRADOR);
		String nit = "90" + UUID.randomUUID().toString().substring(0, 6) + "-2";
		registrar(sesion, "Proveedores del Valle S.A.S.", nit, "3155550000", "");
		long antes = proveedores.count();

		mockMvc.perform(post("/proveedores/nuevo").session(sesion).with(csrf())
						.param("nombre", "Otra Distribuidora S.A.S.")
						.param("nit", nit)
						.param("telefono", "3166660000")
						.param("correo", "")
						.param("direccion", ""))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("DATO_DUPLICADO")));

		assertThat(proveedores.count()).isEqualTo(antes);
	}

	@Test
	@DisplayName("REQ-024 sin teléfono ni correo: rechaza con DATO_INVALIDO porque falta el dato de contacto")
	void sinDatoDeContacto() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.COMPRADOR);
		long antes = proveedores.count();

		mockMvc.perform(post("/proveedores/nuevo").session(sesion).with(csrf())
						.param("nombre", "Sin Contacto S.A.S.")
						.param("nit", "90" + UUID.randomUUID().toString().substring(0, 6) + "-3")
						.param("telefono", "")
						.param("correo", "")
						.param("direccion", "Calle 1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("DATO_INVALIDO")));

		assertThat(proveedores.count()).isEqualTo(antes);
	}

	@Test
	@DisplayName("D-03 el Administrador consulta la lista pero recibe 403 al registrar")
	void administradorConsultaPeroNoRegistra() throws Exception {
		String nit = "90" + UUID.randomUUID().toString().substring(0, 6) + "-4";
		MockHttpSession sesionComprador = iniciarSesion(Rol.COMPRADOR);
		registrar(sesionComprador, "Harinera de Bogota S.A.S.", nit, "3177770000", "");

		MockHttpSession sesionAdmin = iniciarSesion(Rol.ADMINISTRADOR);
		mockMvc.perform(get("/proveedores").session(sesionAdmin))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Harinera de Bogota S.A.S.")));

		mockMvc.perform(get("/proveedores/nuevo").session(sesionAdmin))
				.andExpect(status().isForbidden())
				.andExpect(content().string(containsString("Acceso denegado")));

		mockMvc.perform(post("/proveedores/nuevo").session(sesionAdmin).with(csrf())
						.param("nombre", "No Deberia Existir S.A.S.")
						.param("nit", nit + "X")
						.param("telefono", "3188880000")
						.param("correo", "")
						.param("direccion", ""))
				.andExpect(status().isForbidden());

		assertThat(proveedores.existsByNit(nit + "X")).isFalse();
	}

	private void registrar(MockHttpSession sesion, String nombre, String nit, String telefono, String correo)
			throws Exception {
		mockMvc.perform(post("/proveedores/nuevo").session(sesion).with(csrf())
				.param("nombre", nombre)
				.param("nit", nit)
				.param("telefono", telefono)
				.param("correo", correo)
				.param("direccion", ""))
				.andExpect(status().is3xxRedirection());
	}

}
