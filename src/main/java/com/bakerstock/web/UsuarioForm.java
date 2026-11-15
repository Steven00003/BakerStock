package com.bakerstock.web;

import com.bakerstock.dominio.Rol;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UsuarioForm {

	@NotBlank(message = "DATO_INVALIDO")
	private String nombre;

	@NotBlank(message = "DATO_INVALIDO")
	@Email(message = "DATO_INVALIDO")
	private String correo;

	@NotNull(message = "DATO_INVALIDO")
	private Rol rol;

	/**
	 * NFR-BAK-004: mínimo 8 caracteres con al menos una letra y un número.
	 */
	@NotBlank(message = "DATO_INVALIDO")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "DATO_INVALIDO")
	private String contrasena;

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public Rol getRol() {
		return rol;
	}

	public void setRol(Rol rol) {
		this.rol = rol;
	}

	public String getContrasena() {
		return contrasena;
	}

	public void setContrasena(String contrasena) {
		this.contrasena = contrasena;
	}

}
