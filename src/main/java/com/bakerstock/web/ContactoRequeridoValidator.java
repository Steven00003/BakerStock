package com.bakerstock.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContactoRequeridoValidator implements ConstraintValidator<ContactoRequerido, ProveedorForm> {

	@Override
	public boolean isValid(ProveedorForm formulario, ConstraintValidatorContext contexto) {
		if (formulario == null) {
			return true;
		}
		boolean hayTelefono = formulario.getTelefono() != null && !formulario.getTelefono().isBlank();
		boolean hayCorreo = formulario.getCorreo() != null && !formulario.getCorreo().isBlank();
		return hayTelefono || hayCorreo;
	}

}
