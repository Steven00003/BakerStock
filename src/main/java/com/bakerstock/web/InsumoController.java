package com.bakerstock.web;

import java.math.BigDecimal;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.bakerstock.dominio.AccionAuditoria;
import com.bakerstock.dominio.Categoria;
import com.bakerstock.dominio.EventoAuditoria;
import com.bakerstock.dominio.Insumo;
import com.bakerstock.dominio.ResultadoAuditoria;
import com.bakerstock.dominio.UnidadBase;
import com.bakerstock.repositorio.EventoAuditoriaRepository;
import com.bakerstock.repositorio.InsumoRepository;

import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class InsumoController {

	private final InsumoRepository insumoRepository;
	private final EventoAuditoriaRepository eventoAuditoriaRepository;

	public InsumoController(InsumoRepository insumoRepository,
			EventoAuditoriaRepository eventoAuditoriaRepository) {
		this.insumoRepository = insumoRepository;
		this.eventoAuditoriaRepository = eventoAuditoriaRepository;
	}

	@GetMapping("/insumos/nuevo")
	public String nuevo(Model model) {
		model.addAttribute("formulario", new InsumoForm());
		return "insumos/form";
	}

	@PostMapping("/insumos/nuevo")
	@Transactional
	public String registrar(@Valid @ModelAttribute("formulario") InsumoForm formulario,
			BindingResult resultado, Authentication autenticacion, Model model) {
		if (resultado.hasErrors()) {
			return "insumos/form";
		}
		if (insumoRepository.existsByNombreIgnoreCase(formulario.getNombre())) {
			resultado.rejectValue("nombre", "DATO_DUPLICADO", "DATO_DUPLICADO");
			return "insumos/form";
		}
		Insumo insumo = new Insumo();
		insumo.setNombre(formulario.getNombre().trim());
		insumo.setUnidadBase(UnidadBase.valueOf(formulario.getUnidadBase()));
		insumo.setCategoria(Categoria.valueOf(formulario.getCategoria()));
		insumo.setStockMinimo(formulario.getStockMinimo());
		insumo.setActivo(true);
		insumoRepository.save(insumo);
		eventoAuditoriaRepository.save(EventoAuditoria.registrar(autenticacion.getName(),
				AccionAuditoria.INSUMO_CREADO, ResultadoAuditoria.OK, insumo.getNombre()));
		return "redirect:/insumos/nuevo?creado";
	}

}
