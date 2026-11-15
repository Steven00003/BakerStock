package com.bakerstock.config;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.bakerstock.dominio.Categoria;
import com.bakerstock.dominio.Insumo;
import com.bakerstock.dominio.Lote;
import com.bakerstock.dominio.Proveedor;
import com.bakerstock.dominio.Rol;
import com.bakerstock.dominio.UnidadBase;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.InsumoRepository;
import com.bakerstock.repositorio.LoteRepository;
import com.bakerstock.repositorio.ProveedorRepository;
import com.bakerstock.repositorio.UsuarioRepository;

@Component
@Profile({"dev", "cloud"})
public class DataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
	private static final String DOMINIO = "@bakerstock.test";

	private final UsuarioRepository usuarioRepository;
	private final InsumoRepository insumoRepository;
	private final LoteRepository loteRepository;
	private final ProveedorRepository proveedorRepository;
	private final PasswordEncoder passwordEncoder;

	public DataSeeder(UsuarioRepository usuarioRepository, InsumoRepository insumoRepository,
			LoteRepository loteRepository, ProveedorRepository proveedorRepository,
			PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.insumoRepository = insumoRepository;
		this.loteRepository = loteRepository;
		this.proveedorRepository = proveedorRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		seedUsuarios();
		seedInsumos();
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

	private void seedInsumos() {
		if (insumoRepository.count() > 0) {
			return;
		}
		LocalDate hoy = LocalDate.now();
		Insumo harina = insumo("Harina de trigo", UnidadBase.G, Categoria.HARINAS, "5000");
		Insumo harinaIntegral = insumo("Harina integral", UnidadBase.G, Categoria.HARINAS, "3000");
		Insumo azucar = insumo("Azúcar refinada", UnidadBase.G, Categoria.AZUCARES, "4000");
		Insumo levadura = insumo("Levadura seca", UnidadBase.G, Categoria.LEVADURAS_Y_AGENTES, "500");
		Insumo mantequilla = insumo("Mantequilla", UnidadBase.G, Categoria.GRASAS, "2000");
		Insumo leche = insumo("Leche entera", UnidadBase.ML, Categoria.LACTEOS_Y_HUEVOS, "6000");
		Insumo huevo = insumo("Huevo fresco", UnidadBase.UNIDAD, Categoria.LACTEOS_Y_HUEVOS, "120");
		Insumo bolsa = insumo("Bolsa de papel", UnidadBase.UNIDAD, Categoria.EMPAQUES, "200");
		List.of(harina, harinaIntegral, azucar, levadura, mantequilla, leche, huevo, bolsa)
				.forEach(insumoRepository::save);

		lote(harina, "HAR-001", "20000", hoy.minusDays(60), hoy.minusDays(10));
		lote(harina, "HAR-002", "25000", hoy.minusDays(5), hoy.plusDays(90));
		lote(levadura, "LEV-001", "1500", hoy.minusDays(10), hoy.plusDays(3));
		lote(azucar, "AZU-001", "30000", hoy.minusDays(20), hoy.plusDays(200));
		lote(mantequilla, "MAN-001", "8000", hoy.minusDays(15), hoy.plusDays(45));
		lote(huevo, "HUE-001", "150", hoy.minusDays(2), hoy.plusDays(12));
	}

	private Insumo insumo(String nombre, UnidadBase unidadBase, Categoria categoria, String stockMinimo) {
		Insumo insumo = new Insumo();
		insumo.setNombre(nombre);
		insumo.setUnidadBase(unidadBase);
		insumo.setCategoria(categoria);
		insumo.setStockMinimo(new BigDecimal(stockMinimo));
		insumo.setActivo(true);
		return insumo;
	}

	private void lote(Insumo insumo, String numero, String cantidad, LocalDate recepcion, LocalDate vencimiento) {
		Lote lote = new Lote();
		lote.setInsumo(insumo);
		lote.setNumeroLote(numero);
		lote.setCantidadInicial(new BigDecimal(cantidad));
		lote.setCantidadActual(new BigDecimal(cantidad));
		lote.setCostoUnitario(new BigDecimal("1.500"));
		lote.setFechaRecepcion(recepcion);
		lote.setFechaVencimiento(vencimiento);
		loteRepository.save(lote);
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