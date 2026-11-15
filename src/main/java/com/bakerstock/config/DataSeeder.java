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

import com.bakerstock.dominio.Proveedor;
import com.bakerstock.dominio.Rol;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.ProveedorRepository;
import com.bakerstock.repositorio.UsuarioRepository;

@Component
@Profile({"dev", "cloud"})
public class DataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
	private static final String DOMINIO = "@bakerstock.test";

	private final UsuarioRepository usuarioRepository;
	private final ProveedorRepository proveedorRepository;
	private final PasswordEncoder passwordEncoder;

	public DataSeeder(UsuarioRepository usuarioRepository, ProveedorRepository proveedorRepository,
			PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.proveedorRepository = proveedorRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		seedUsuarios();
		seedProveedores();
	}

	private void seedProveedores() {
		if (proveedorRepository.count() > 0) {
			return;
		}
		for (String[] datos : new String[][] {
				{"Distribuidora de Harinas del Valle S.A.S.", "900123456-1", "3001234567", "ventas@harinasdelvalle.test", "Calle 45 # 23-16, Bogota"},
				{"Humedos y Lacteos Andinos S.A.S.", "900234567-2", "3102345678", "pedidos@andinoslacteos.test", "Avenida 68 # 40-10, Bogota"},
				{"Empaques de Papel Sol Ltda.", "900345678-3", "3203456789", "contacto@empaquessol.test", "Carrera 30 # 5-51, Medellin"}}) {
			Proveedor proveedor = new Proveedor();
			proveedor.setNombre(datos[0]);
			proveedor.setNit(datos[1]);
			proveedor.setTelefono(datos[2]);
			proveedor.setCorreo(datos[3]);
			proveedor.setDireccion(datos[4]);
			proveedor.setActivo(true);
			proveedorRepository.save(proveedor);
		}
	}

	private void seedUsuarios() {
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
