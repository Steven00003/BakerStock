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
import com.bakerstock.repositorio.InsumoRepository;
import com.bakerstock.repositorio.LoteRepository;
import com.bakerstock.repositorio.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InsumoRegistroTest {

	private static final String CLAVE = "ClaveSegura1*";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private InsumoRepository insumos;

	@Autowired
	private LoteRepository lotes;

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
	@DisplayName("AC-022.1 el Almacenista registra «Harina de trigo» (g, HARINAS, mínimo 5000): se crea con stock 0 y evento INSUMO_CREADO")
	void registraInsumoValido() throws Exception {
		// Nombre con sufijo único: los datos persisten entre pruebas de la misma suite.
		String nombre = "Harina de trigo-" + UUID.randomUUID();
		String correoAlmacenista = crearUsuario(Rol.ALMACENISTA);
		MockHttpSession sesion = iniciarSesion(correoAlmacenista);
		long lotesAntes = lotes.count();

		mockMvc.perform(post("/insumos/nuevo").session(sesion).with(csrf())
						.param("nombre", nombre)
						.param("unidadBase", "G")
						.param("categoria", "HARINAS")
						.param("stockMinimo", "5000"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/insumos/nuevo?creado"));

		var insumo = insumos.findByNombreIgnoreCase(nombre).orElseThrow();
		assertThat(insumo.isActivo()).isTrue();
		assertThat(insumo.getStockMinimo()).isEqualByComparingTo("5000");
		assertThat(lotes.existsByInsumoId(insumo.getId())).isFalse();

		EventoAuditoria evento = auditoria.findByUsuarioCorreoAndAccion(correoAlmacenista, AccionAuditoria.INSUMO_CREADO)
				.stream()
				.findFirst()
				.orElseThrow();
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoria.OK);
		assertThat(evento.getDetalle()).isEqualTo(nombre);

		assertThat(lotes.count()).isEqualTo(lotesAntes);
	}

	@Test
	@DisplayName("AC-022.2 stock mínimo −1: rechaza con DATO_INVALIDO y no crea el insumo")
	void stockMinimoNegativo() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.ALMACENISTA);
		long antes = insumos.count();

		mockMvc.perform(post("/insumos/nuevo").session(sesion).with(csrf())
						.param("nombre", "Azúcar refinada")
						.param("unidadBase", "G")
						.param("categoria", "AZUCARES")
						.param("stockMinimo", "-1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("DATO_INVALIDO")));

		assertThat(insumos.count()).isEqualTo(antes);
	}

	@Test
	@DisplayName("AC-022.3 nombre repetido sin distinguir mayúsculas: rechaza con DATO_DUPLICADO y no crea otro insumo")
	void nombreDuplicado() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.ALMACENISTA);
		String base = "harina de avena-" + UUID.randomUUID();
		registrar(sesion, base, "G", "HARINAS", "5000");
		long antes = insumos.count();

		mockMvc.perform(post("/insumos/nuevo").session(sesion).with(csrf())
						.param("nombre", base.toUpperCase())
						.param("unidadBase", "G")
						.param("categoria", "HARINAS")
						.param("stockMinimo", "3000"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("DATO_DUPLICADO")));

		assertThat(insumos.count()).isEqualTo(antes);
	}

	@Test
	@DisplayName("AC-023.1 unidad «kg»: rechaza con «Use g, ml o unidad» (DATO_INVALIDO)")
	void unidadInvalida() throws Exception {
		MockHttpSession sesion = iniciarSesion(Rol.ALMACENISTA);
		long antes = insumos.count();

		mockMvc.perform(post("/insumos/nuevo").session(sesion).with(csrf())
						.param("nombre", "Harina integral")
						.param("unidadBase", "kg")
						.param("categoria", "HARINAS")
						.param("stockMinimo", "1000"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.allOf(
						containsString("Use g, ml o unidad"),
						containsString("DATO_INVALIDO"))));

		assertThat(insumos.count()).isEqualTo(antes);
	}

	private void registrar(MockHttpSession sesion, String nombre, String unidad, String categoria, String minimo)
			throws Exception {
		mockMvc.perform(post("/insumos/nuevo").session(sesion).with(csrf())
				.param("nombre", nombre)
				.param("unidadBase", unidad)
				.param("categoria", categoria)
				.param("stockMinimo", minimo))
				.andExpect(status().is3xxRedirection());
	}

}
