package com.bakerstock.web;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.bakerstock.dominio.AccionAuditoria;
import com.bakerstock.dominio.EventoAuditoria;
import com.bakerstock.dominio.Proveedor;
import com.bakerstock.dominio.ResultadoAuditoria;
import com.bakerstock.repositorio.EventoAuditoriaRepository;
import com.bakerstock.repositorio.ProveedorRepository;

import jakarta.validation.Valid;

@Controller
public class ProveedorController {

	private final ProveedorRepository proveedorRepository;
	private final EventoAuditoriaRepository eventoAuditoriaRepository;

	public ProveedorController(ProveedorRepository proveedorRepository,
			EventoAuditoriaRepository eventoAuditoriaRepository) {
		this.proveedorRepository = proveedorRepository;
		this.eventoAuditoriaRepository = eventoAuditoriaRepository;
	}

	@GetMapping("/proveedores")
	public String lista(Model model) {
		List<Proveedor> proveedores = proveedorRepository.findAllByOrderByNombreAsc();
		model.addAttribute("proveedores", proveedores);
		return "proveedores/lista";
	}

	@GetMapping("/proveedores/nuevo")
	public String nuevo(Model model) {
		model.addAttribute("formulario", new ProveedorForm());
		return "proveedores/form";
	}

	@PostMapping("/proveedores/nuevo")
	@Transactional
	public String registrar(@Valid @ModelAttribute("formulario") ProveedorForm formulario,
			BindingResult resultado, Authentication autenticacion, Model model) {
		if (resultado.hasErrors()) {
			return "proveedores/form";
		}
		String nit = formulario.getNit().trim();
		if (proveedorRepository.existsByNit(nit)) {
			resultado.rejectValue("nit", "DATO_DUPLICADO", "DATO_DUPLICADO");
			return "proveedores/form";
		}
		Proveedor proveedor = new Proveedor();
		proveedor.setNombre(formulario.getNombre().trim());
		proveedor.setNit(nit);
		proveedor.setTelefono(texto(formulario.getTelefono()));
		proveedor.setCorreo(texto(formulario.getCorreo()));
		proveedor.setDireccion(texto(formulario.getDireccion()));
		proveedor.setActivo(true);
		proveedorRepository.save(proveedor);
		eventoAuditoriaRepository.save(EventoAuditoria.registrar(autenticacion.getName(),
				AccionAuditoria.PROVEEDOR_CREADO, ResultadoAuditoria.OK, proveedor.getNombre()));
		return "redirect:/proveedores?creado";
	}

	private String texto(String valor) {
		return (valor == null || valor.isBlank()) ? null : valor.trim();
	}

}
