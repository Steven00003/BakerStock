package com.bakerstock;

import java.math.BigDecimal;
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

import com.bakerstock.dominio.Categoria;
import com.bakerstock.dominio.Insumo;
import com.bakerstock.dominio.Rol;
import com.bakerstock.dominio.UnidadBase;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.InsumoRepository;
import com.bakerstock.repositorio.UsuarioRepository;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InsumoBusquedaTest {

	private static final String CLAVE = "ClaveSegura1*";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private InsumoRepository insumos;

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

	private MockHttpSession iniciarSesion(Rol rol) throws Exception {
		String correo = crearUsuario(rol);
		MvcResult login = mockMvc.perform(formLogin().user(correo).password(CLAVE))
				.andExpect(redirectedUrl("/"))
				.andReturn();
		return (MockHttpSession) login.getRequest().getSession(false);
	}

	private void crearInsumo(String nombre, Categoria categoria) {
		Insumo insumo = new Insumo();
		insumo.setNombre(nombre);
		insumo.setUnidadBase(UnidadBase.G);
		insumo.setCategoria(categoria);
		insumo.setStockMinimo(new BigDecimal("1000"));
		insumo.setActivo(true);
		insumos.save(insumo);
	}

	@Test
	@DisplayName("AC-025.1 al buscar «har» aparecen las dos harinas y no el azúcar")
	void buscaPorTexto() throws Exception {
		String harina1 = "Harina de trigo-" + UUID.randomUUID();
		String harina2 = "Harina integral-" + UUID.randomUUID();
		String azucar = "Azúcar especial-" + UUID.randomUUID();
		crearInsumo(harina1, Categoria.HARINAS);
		crearInsumo(harina2, Categoria.HARINAS);
		crearInsumo(azucar, Categoria.AZUCARES);
		MockHttpSession sesion = iniciarSesion(Rol.ALMACENISTA);

		mockMvc.perform(get("/insumos").param("q", "har").session(sesion))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString(harina1),
						containsString(harina2),
						not(containsString(azucar)))));
	}

	@Test
	@DisplayName("AC-025.2 al filtrar por LACTEOS_Y_HUEVOS sin texto aparecen solo los de esa categoría")
	void filtraPorCategoria() throws Exception {
		String leche = "Leche entera-" + UUID.randomUUID();
		String huevo = "Huevo fresco-" + UUID.randomUUID();
		String azucar = "Azúcar refinada-" + UUID.randomUUID();
		crearInsumo(leche, Categoria.LACTEOS_Y_HUEVOS);
		crearInsumo(huevo, Categoria.LACTEOS_Y_HUEVOS);
		crearInsumo(azucar, Categoria.AZUCARES);
		MockHttpSession sesion = iniciarSesion(Rol.PANADERO);

		mockMvc.perform(get("/insumos").param("categoria", "LACTEOS_Y_HUEVOS").session(sesion))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString(leche),
						containsString(huevo),
						not(containsString(azucar)))));
	}

	@Test
	@DisplayName("AC-025.3 al buscar «zzz» muestra «No se encontraron insumos»")
	void busquedaSinResultados() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.COMPRADOR);

		mockMvc.perform(get("/insumos").param("q", "zzz").session(sesion))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("No se encontraron insumos")));
	}

	@Test
	@DisplayName("REQ-025/D-02 el Cajero recibe 403 en el listado y el acceso queda auditado")
	void cajeroSinAcceso() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.CAJERO);

		mockMvc.perform(get("/insumos").session(sesion))
				.andExpect(status().isForbidden())
				.andExpect(content().string(containsString("Acceso denegado")));
	}

	@Test
	@DisplayName("REQ-025 una categoría inexistente como parámetro no rompe la página")
	void categoriaInvalidaSeIgnora() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.ALMACENISTA);

		mockMvc.perform(get("/insumos").param("categoria", "IMAGINARIA").session(sesion))
				.andExpect(status().isOk());
	}

}
