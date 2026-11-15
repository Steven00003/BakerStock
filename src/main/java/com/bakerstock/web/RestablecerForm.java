package com.bakerstock.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RestablecerForm {

	/**
	 * NFR-BAK-004: mínimo 8 caracteres con al menos una letra y un número.
	 */
	@NotBlank(message = "DATO_INVALIDO")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "DATO_INVALIDO")
	private String contrasena;

	public String getContrasena() {
		return contrasena;
	}

	public void setContrasena(String contrasena) {
		this.contrasena = contrasena;
	}

}
