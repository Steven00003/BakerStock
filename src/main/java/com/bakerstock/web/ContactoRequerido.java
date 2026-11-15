package com.bakerstock.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * REQ-024: el registro de proveedor exige al menos un dato de contacto
 * (teléfono o correo).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ContactoRequeridoValidator.class)
public @interface ContactoRequerido {

	String message() default "DATO_INVALIDO";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
