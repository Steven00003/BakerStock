package com.bakerstock.web;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bakerstock.dominio.AccionAuditoria;
import com.bakerstock.dominio.EventoAuditoria;
import com.bakerstock.dominio.ResultadoAuditoria;
import com.bakerstock.dominio.TokenRecuperacion;
import com.bakerstock.dominio.Usuario;
import com.bakerstock.repositorio.EventoAuditoriaRepository;
import com.bakerstock.repositorio.TokenRecuperacionRepository;
import com.bakerstock.repositorio.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
public class RecuperacionController {

	private static final Logger log = LoggerFactory.getLogger(RecuperacionController.class);

	static final String MENSAJE_NEUTRO = "Si el correo está registrado, se generó un enlace de recuperación en el log de la aplicación.";
	static final String MENSAJE_TOKEN_INVALIDO = "El enlace de recuperación no es válido o ya expiró.";

	private final UsuarioRepository usuarioRepository;
	private final TokenRecuperacionRepository tokenRecuperacionRepository;
	private final EventoAuditoriaRepository eventoAuditoriaRepository;
	private final PasswordEncoder passwordEncoder;

	public RecuperacionController(UsuarioRepository usuarioRepository,
			TokenRecuperacionRepository tokenRecuperacionRepository,
			EventoAuditoriaRepository eventoAuditoriaRepository,
			PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.tokenRecuperacionRepository = tokenRecuperacionRepository;
		this.eventoAuditoriaRepository = eventoAuditoriaRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/recuperar")
	public String solicitar() {
		return "recuperar";
	}

	@PostMapping("/recuperar")
	public String crearToken(@RequestParam(required = false) String correo,
			HttpServletRequest request, Model model) {
		model.addAttribute("mensaje", MENSAJE_NEUTRO);
		String normalizado = correo == null ? "" : correo.trim().toLowerCase();
		usuarioRepository.findByCorreoIgnoreCase(normalizado)
				.filter(Usuario::isActivo)
				.ifPresent(usuario -> generarEnlace(usuario, request));
		return "recuperar";
	}

	private void generarEnlace(Usuario usuario, HttpServletRequest request) {
		String tokenClaro = TokenRecuperacion.generarTokenClaro();
		tokenRecuperacionRepository.save(TokenRecuperacion.crear(usuario,
				TokenRecuperacion.hashDe(tokenClaro), LocalDateTime.now()));
		eventoAuditoriaRepository.save(EventoAuditoria.registrar(usuario.getCorreo(),
				AccionAuditoria.RECUPERACION_SOLICITADA, ResultadoAuditoria.OK,
				usuario.getCorreo()));
		String url = raizDe(request) + "/restablecer?token=" + tokenClaro;
		log.info("Enlace de recuperación para {}: {}", usuario.getCorreo(), url);
	}

	private String raizDe(HttpServletRequest request) {
		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
				+ request.getContextPath();
	}

	@GetMapping("/restablecer")
	public String formularioRestablecer(@RequestParam(required = false) String token, Model model) {
		TokenRecuperacion tokenRecuperacion = buscarTokenUsable(token);
		if (tokenRecuperacion == null) {
			registrarRechazo(token);
			model.addAttribute("tokenValido", false);
			return "restablecer";
		}
		model.addAttribute("tokenValido", true);
		model.addAttribute("token", token);
		model.addAttribute("formulario", new RestablecerForm());
		return "restablecer";
	}

	@PostMapping("/restablecer")
	public String restablecer(@RequestParam(required = false) String token,
			@Valid @ModelAttribute("formulario") RestablecerForm formulario,
			BindingResult resultado, Model model) {
		TokenRecuperacion tokenRecuperacion = buscarTokenUsable(token);
		if (tokenRecuperacion == null) {
			registrarRechazo(token);
			model.addAttribute("tokenValido", false);
			return "restablecer";
		}
		if (resultado.hasErrors()) {
			model.addAttribute("tokenValido", true);
			model.addAttribute("token", token);
			return "restablecer";
		}
		Usuario usuario = tokenRecuperacion.getUsuario();
		usuario.setContrasenaHash(passwordEncoder.encode(formulario.getContrasena()));
		usuarioRepository.save(usuario);
		tokenRecuperacion.marcarUsado();
		tokenRecuperacionRepository.save(tokenRecuperacion);
		eventoAuditoriaRepository.save(EventoAuditoria.registrar(usuario.getCorreo(),
				AccionAuditoria.CONTRASENA_RESTABLECIDA, ResultadoAuditoria.OK,
				"Restablecimiento con token de un solo uso"));
		return "redirect:/login?restablecido";
	}

	private TokenRecuperacion buscarTokenUsable(String tokenClaro) {
		if (tokenClaro == null) {
			return null;
		}
		return tokenRecuperacionRepository.findByTokenHash(TokenRecuperacion.hashDe(tokenClaro))
				.filter(t -> t.esUsableEn(LocalDateTime.now()))
				.orElse(null);
	}

	private void registrarRechazo(String tokenClaro) {
		String motivo = "INVALIDO";
		String correo = "anonimo";
		if (tokenClaro != null) {
			Optional<TokenRecuperacion> encontrado = tokenRecuperacionRepository
					.findByTokenHash(TokenRecuperacion.hashDe(tokenClaro));
			if (encontrado.isPresent()) {
				TokenRecuperacion token = encontrado.get();
				correo = token.getUsuario().getCorreo();
				motivo = token.isUsado() ? "USADO" : "VENCIDO";
			}
		}
		eventoAuditoriaRepository.save(EventoAuditoria.registrar(correo,
				AccionAuditoria.RECUPERACION_RECHAZADA, ResultadoAuditoria.RECHAZADO, motivo));
	}

}
