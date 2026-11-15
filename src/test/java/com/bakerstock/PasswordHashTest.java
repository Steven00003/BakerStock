package com.bakerstock;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.bakerstock.dominio.Rol;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PasswordHashTest {

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PasswordEncoder encoder;

	@Test
	@DisplayName("AC-N002.1 la contraseña se guarda con BCrypt y no aparece en claro")
	void hashEsBcryptSinTextoClaro() {
		String clara = "ClaraSecreta42*";
		Usuario usuario = new Usuario();
		usuario.setNombre("PANADERO");
		usuario.setCorreo("hash-" + UUID.randomUUID() + "@bakerstock.test");
		usuario.setContrasenaHash(encoder.encode(clara));
		usuario.setRol(Rol.PANADERO);
		usuario.setActivo(true);
		Usuario guardado = usuarios.save(usuario);
		Usuario leido = usuarios.findById(guardado.getId()).orElseThrow();

		String hash = leido.getContrasenaHash();
		assertThat(hash).matches("^\\$2[aby]\\$\\S{50,}$");
		assertThat(hash).doesNotContain(clara);
		assertThat(encoder.matches(clara, hash)).isTrue();
	}

}
