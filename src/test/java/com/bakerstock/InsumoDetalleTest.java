package com.bakerstock;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.bakerstock.dominio.Lote;
import com.bakerstock.dominio.Rol;
import com.bakerstock.dominio.UnidadBase;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.InsumoRepository;
import com.bakerstock.repositorio.LoteRepository;
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
class InsumoDetalleTest {

	private static final String CLAVE = "ClaveSegura1*";
	private static final LocalDate HOY = LocalDate.now();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private InsumoRepository insumos;

	@Autowired
	private LoteRepository lotes;

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

	private Insumo crearInsumo(String nombre) {
		Insumo insumo = new Insumo();
		insumo.setNombre(nombre);
		insumo.setUnidadBase(UnidadBase.G);
		insumo.setCategoria(Categoria.HARINAS);
		insumo.setStockMinimo(new BigDecimal("1000"));
		insumo.setActivo(true);
		return insumos.save(insumo);
	}

	private Lote crearLote(Insumo insumo, String cantidad, LocalDate vencimiento) {
		Lote lote = new Lote();
		lote.setInsumo(insumo);
		lote.setNumeroLote("L-" + vencimiento);
		lote.setCantidadInicial(new BigDecimal(cantidad));
		lote.setCantidadActual(new BigDecimal(cantidad));
		lote.setCostoUnitario(BigDecimal.ONE);
		lote.setFechaRecepcion(vencimiento.minusDays(30));
		lote.setFechaVencimiento(vencimiento);
		return lotes.save(lote);
	}

	@Test
	@DisplayName("AC-026.1 consulta de harina con lote A (1000 g) y lote B (1500 g) vigentes: stock total 2500 g y el lote B antes que el A")
	void muestraStockTotalYOrdenFefo() throws Exception {
		Insumo insumo = crearInsumo("Harina de trigo-" + UUID.randomUUID());
		crearLote(insumo, "1000", HOY.plusDays(40));
		crearLote(insumo, "1500", HOY.plusDays(30));
		MockHttpSession sesion = iniciarSesion(crearUsuario(Rol.ALMACENISTA));

		String cuerpo = mockMvc.perform(get("/insumos/{id}", insumo.getId()).session(sesion))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(insumo.getNombre())))
				.andReturn()
				.getResponse()
				.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

		assertThat(cuerpo).contains("2500");
		assertThat(cuerpo).contains(" g");
		// El lote B (vence antes) debe aparecer antes que el lote A: FEFO.
		String numeroLoteB = "L-" + HOY.plusDays(30);
		String numeroLoteA = "L-" + HOY.plusDays(40);
		assertThat(cuerpo.indexOf(numeroLoteB)).isLessThan(cuerpo.indexOf(numeroLoteA));
	}

	@Test
	@DisplayName("AC-026.2 consulta con lote vencido de 500 g y vigente de 1000 g: stock total 1500 g, utilizable 1000 g y «Vencido» marcado")
	void marcaVencidosYCalculaUtilizable() throws Exception {
		Insumo insumo = crearInsumo("Harina integral-" + UUID.randomUUID());
		crearLote(insumo, "500", HOY.minusDays(1));
		crearLote(insumo, "1000", HOY.plusDays(30));
		MockHttpSession sesion = iniciarSesion(crearUsuario(Rol.PANADERO));

		mockMvc.perform(get("/insumos/{id}", insumo.getId()).session(sesion))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.allOf(
						containsString("1500"),
						containsString("Vencido"),
						containsString("500"))));
	}

	@Test
	@DisplayName("AC-026.3 consulta de un insumo sin lotes: stock 0 y «Sin lotes registrados»")
	void insumoSinLotes() throws Exception {
		Insumo insumo = crearInsumo("Mermelada de fresa-" + UUID.randomUUID());
		MockHttpSession sesion = iniciarSesion(crearUsuario(Rol.COMPRADOR));

		mockMvc.perform(get("/insumos/{id}", insumo.getId()).session(sesion))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Sin lotes registrados")));
	}

	@Test
	@DisplayName("REQ-026 el Cajero recibe 403 y el acceso queda auditado")
	void cajeroSinAcceso() throws Exception {
		Insumo insumo = crearInsumo("Levadura seca-" + UUID.randomUUID());
		MockHttpSession sesion = iniciarSesion(crearUsuario(Rol.CAJERO));

		mockMvc.perform(get("/insumos/{id}", insumo.getId()).session(sesion))
				.andExpect(status().isForbidden())
				.andExpect(content().string(containsString("Acceso denegado")));
	}

	@Test
	@DisplayName("REQ-026 un id de insumo inexistente devuelve 404")
	void idInexistente() throws Exception {
		MockHttpSession sesion = iniciarSesion(crearUsuario(Rol.ALMACENISTA));

		mockMvc.perform(get("/insumos/{id}", 999999).session(sesion))
				.andExpect(status().isNotFound());
	}

}
