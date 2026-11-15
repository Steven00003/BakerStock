package com.bakerstock.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final AccesoDenegadoHandler accesoDenegadoHandler;

	public SecurityConfig(AccesoDenegadoHandler accesoDenegadoHandler) {
		this.accesoDenegadoHandler = accesoDenegadoHandler;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/login", "/recuperar", "/restablecer").permitAll()
						.requestMatchers("/admin/usuarios/**").hasRole("ADMINISTRADOR")
						.requestMatchers("/proveedores").hasAnyRole("COMPRADOR", "ADMINISTRADOR")
						.requestMatchers("/proveedores/nuevo").hasRole("COMPRADOR")
						.requestMatchers("/insumos/nuevo").hasRole("ALMACENISTA")
						.requestMatchers("/insumos", "/insumos/*")
								.hasAnyRole("ADMINISTRADOR", "COMPRADOR", "ALMACENISTA", "PANADERO")
						.requestMatchers("/acceso-denegado").authenticated()
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.defaultSuccessUrl("/", true)
						.failureUrl("/login?error")
						.permitAll())
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout")
						.permitAll())
				.exceptionHandling(ex -> ex.accessDeniedHandler(accesoDenegadoHandler));
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
