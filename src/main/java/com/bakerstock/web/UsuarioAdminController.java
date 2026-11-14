package com.bakerstock.web;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.bakerstock.dominio.AccionAuditoria;
import com.bakerstock.dominio.EventoAuditoria;
import com.bakerstock.dominio.ResultadoAuditoria;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.EventoAuditoriaRepository;
import com.bakerstock.repositorio.UsuarioRepository;

import jakarta.validation.Valid;

@Controller
public class UsuarioAdminController {

	private final UsuarioRepository usuarioRepository;
	private final EventoAuditoriaRepository eventoAuditoriaRepository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioAdminController(UsuarioRepository usuarioRepository,
			EventoAuditoriaRepository eventoAuditoriaRepository,
			PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.eventoAuditoriaRepository = eventoAuditoriaRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/admin/usuarios")
	public String lista(Model model) {
		model.addAttribute("usuarios", usuarioRepository.findAll());
		return "admin/usuarios/lista";
	}

	@GetMapping("/admin/usuarios/nuevo")
	public String nuevo(Model model) {
		model.addAttribute("formulario", new UsuarioForm());
		return "admin/usuarios/form";
	}

	@PostMapping("/admin/usuarios/nuevo")
	public String crear(@Valid @ModelAttribute("formulario") UsuarioForm formulario,
			BindingResult resultado, Authentication autenticacion) {
		if (resultado.hasErrors()) {
			return "admin/usuarios/form";
		}
		if (usuarioRepository.findByCorreoIgnoreCase(formulario.getCorreo()).isPresent()) {
			resultado.rejectValue("correo", "DATO_DUPLICADO", "DATO_DUPLICADO");
			return "admin/usuarios/form";
		}
		Usuario usuario = new Usuario();
		usuario.setNombre(formulario.getNombre().trim());
		usuario.setCorreo(formulario.getCorreo());
		usuario.setContrasenaHash(passwordEncoder.encode(formulario.getContrasena()));
		usuario.setRol(formulario.getRol());
		usuario.setActivo(true);
		usuarioRepository.save(usuario);
		eventoAuditoriaRepository.save(EventoAuditoria.registrar(autenticacion.getName(),
				AccionAuditoria.USUARIO_CREADO, ResultadoAuditoria.OK, usuario.getCorreo()));
		return "redirect:/admin/usuarios";
	}

	@PostMapping("/admin/usuarios/{id}/desactivar")
	public String desactivar(@PathVariable Long id, Authentication autenticacion) {
		Usuario actual = usuarioRepository.findByCorreoIgnoreCase(autenticacion.getName()).orElseThrow();
		if (Objects.equals(actual.getId(), id)) {
			eventoAuditoriaRepository.save(EventoAuditoria.registrar(autenticacion.getName(),
					AccionAuditoria.USUARIO_DESACTIVADO, ResultadoAuditoria.RECHAZADO,
					"No se puede desactivar la propia cuenta"));
			return "redirect:/admin/usuarios?error=propia";
		}
		Usuario objetivo = usuarioRepository.findById(id).orElseThrow();
		objetivo.setActivo(false);
		usuarioRepository.save(objetivo);
		eventoAuditoriaRepository.save(EventoAuditoria.registrar(autenticacion.getName(),
				AccionAuditoria.USUARIO_DESACTIVADO, ResultadoAuditoria.OK, objetivo.getCorreo()));
		return "redirect:/admin/usuarios";
	}

	@PostMapping("/admin/usuarios/{id}/activar")
	public String activar(@PathVariable Long id, Authentication autenticacion) {
		Usuario objetivo = usuarioRepository.findById(id).orElseThrow();
		objetivo.setActivo(true);
		usuarioRepository.save(objetivo);
		eventoAuditoriaRepository.save(EventoAuditoria.registrar(autenticacion.getName(),
				AccionAuditoria.USUARIO_ACTIVADO, ResultadoAuditoria.OK, objetivo.getCorreo()));
		return "redirect:/admin/usuarios";
	}

}
