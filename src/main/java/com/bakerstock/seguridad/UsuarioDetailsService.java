package com.bakerstock.seguridad;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.UsuarioRepository;

@Component
public class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
		Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
				.orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
		return User.withUsername(usuario.getCorreo())
				.password(usuario.getContrasenaHash())
				.disabled(!usuario.isActivo())
				.authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
				.build();
	}

}
