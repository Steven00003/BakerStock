package com.bakerstock.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@ContactoRequerido
public class ProveedorForm {

	@NotBlank(message = "DATO_INVALIDO")
	private String nombre;

	@NotBlank(message = "DATO_INVALIDO")
	private String nit;

	private String telefono;

	@Email(message = "DATO_INVALIDO")
	private String correo;

	private String direccion;

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getNit() {
		return nit;
	}

	public void setNit(String nit) {
		this.nit = nit;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

}
