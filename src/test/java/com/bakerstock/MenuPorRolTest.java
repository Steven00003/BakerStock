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

import com.bakerstock.dominio.Rol;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.UsuarioRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MenuPorRolTest {

	private static final String CLAVE = "ClaveSegura1*";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarios;

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
	@DisplayName("AC-021.1 el menú del Comprador muestra Proveedores e Insumos y no muestra Usuarios")
	void menuComprador() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.COMPRADOR);
		mockMvc.perform(get("/").session(sesion))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.allOf(
						org.hamcrest.Matchers.containsString("Proveedores"),
						org.hamcrest.Matchers.containsString("Insumos"),
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Usuarios")))));
	}

	@Test
	@DisplayName("AC-021.2 el menú del Administrador muestra Usuarios, Proveedores e Insumos")
	void menuAdministrador() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.ADMINISTRADOR);
		mockMvc.perform(get("/").session(sesion))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.allOf(
						org.hamcrest.Matchers.containsString("Usuarios"),
						org.hamcrest.Matchers.containsString("Proveedores"),
						org.hamcrest.Matchers.containsString("Insumos"))));
	}

	@Test
	@DisplayName("REQ-021 el menú del Almacenista muestra solo Insumos (sin Usuarios ni Proveedores)")
	void menuAlmacenista() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.ALMACENISTA);
		mockMvc.perform(get("/").session(sesion))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.allOf(
						org.hamcrest.Matchers.containsString("Insumos"),
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Usuarios")),
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Proveedores")))));
	}

	@Test
	@DisplayName("REQ-021 el menú del Cajero muestra solo Inicio")
	void menuCajero() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.CAJERO);
		mockMvc.perform(get("/").session(sesion))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.allOf(
						org.hamcrest.Matchers.containsString("Inicio"),
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Usuarios")),
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Proveedores")),
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Insumos")))));
	}

}
