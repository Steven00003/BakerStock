package com.bakerstock.config;

import java.security.SecureRandom;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.bakerstock.dominio.Rol;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.UsuarioRepository;

@Component
@Profile({"dev", "cloud"})
public class DataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
	private static final String DOMINIO = "@bakerstock.test";

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public DataSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (usuarioRepository.count() > 0) {
			return;
		}
		String contrasena = System.getenv("BAKERSTOCK_SEED_PASSWORD");
		if (contrasena == null || contrasena.isBlank()) {
			contrasena = aleatoria();
			log.info("BAKERSTOCK_SEED_PASSWORD no definida; contraseña sembrada (se imprime una sola vez): {}",
					contrasena);
		}
		for (Rol rol : Rol.values()) {
			crear(rol, contrasena);
		}
	}

	private void crear(Rol rol, String contrasena) {
		Usuario usuario = new Usuario();
		usuario.setNombre(nombre(rol));
		usuario.setCorreo(rol.name().toLowerCase() + DOMINIO);
		usuario.setContrasenaHash(passwordEncoder.encode(contrasena));
		usuario.setRol(rol);
		usuario.setActivo(true);
		usuarioRepository.save(usuario);
	}

	private String nombre(Rol rol) {
		return rol.name().charAt(0) + rol.name().substring(1).toLowerCase();
	}

	private String aleatoria() {
		byte[] bytes = new byte[12];
		new SecureRandom().nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

}
