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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLogoutTest {

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

	@Test
	@DisplayName("AC-017.1 tras cerrar sesión, abrir / con la sesión antigua redirige a /login")
	void logoutInvalidaSesion() throws Exception {
		String correo = crearUsuarioActivo(Rol.COMPRADOR);
		MvcResult login = mockMvc.perform(formLogin().user(correo).password(CLAVE))
				.andExpect(redirectedUrl("/"))
				.andReturn();
		MockHttpSession sesion = (MockHttpSession) login.getRequest().getSession(false);
		assertThat(sesion).isNotNull();

		mockMvc.perform(post("/logout").session(sesion).with(csrf()))
				.andExpect(redirectedUrl("/login?logout"));

		mockMvc.perform(get("/").session(sesion))
				.andExpect(redirectedUrlPattern("**/login"));
	}

}
