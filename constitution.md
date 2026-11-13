# Constitución de BakerStock

**Versión:** 1.0 (borrador para Puerta 0) · **Fecha:** 2026-11-13
**Equipo:** Ana Sofía Gómez Arguello (Scrum Master y Developer) · David Steven Patiño Rueda (Developer)
**Product Owner:** Julián Barney

Esta constitución fija los principios que no se negocian durante el proyecto. Cuando una decisión técnica entre en conflicto con un principio, gana el principio. La IA que trabaje en el repositorio la lee junto con `AGENTS.md`.

## Product Goal

Que ningún insumo ni producto de la panadería se mueva sin registro: toda entrada, producción y venta queda registrada por la persona que su rol autoriza, el stock nunca es negativo y cada operación queda auditada.

## Principios

1. **La especificación es la fuente de verdad.** Todo cambio de comportamiento empieza en `specs/001-BakerStock/spec.md` (como delta ADDED, MODIFIED o REMOVED) y después llega al código. Un hotfix urgente se permite, pero deja una tarea para sincronizar la especificación en menos de 48 horas.
2. **El stock nunca es negativo (RN-03).** Se garantiza en tres puntos: validación antes de operar, transacción atómica y restricción en la base de datos.
3. **Los movimientos son inmutables (RN-06).** No se editan ni se eliminan; se corrigen con un movimiento contrario. No existen rutas, métodos ni consultas `UPDATE` o `DELETE` sobre movimientos.
4. **Mínimo privilegio por rol.** Cada rol hace solo lo que su función exige. Toda acción no permitida se rechaza y queda auditada (REQ-BAK-008).
5. **Primero la prueba.** Cada criterio de aceptación tiene al menos una prueba automatizada, escrita antes del código y vista en rojo antes de ponerse en verde. Los casos de borde se prueban.
6. **Alcance mínimo.** Si ningún requisito lo pide, no se programa y no se añade la dependencia. Los no-objetivos (DIAN, contabilidad, pago en línea, domicilios, app móvil) se respetan.
7. **Simplicidad arquitectónica.** Una sola aplicación modular. Los controladores usan los repositorios de Spring Data JPA directamente; no hay capa `service` (ADR-004). Las reglas viven en el dominio (entidades) y en las transacciones.
8. **La IA es una herramienta, no un rol.** Quien dirige a la IA responde por el resultado; otra persona lee el diff completo (cuatro ojos); la IA no modifica la especificación, solo sugiere mejoras al Product Owner; todo uso queda en `docs/ia/bitacora.md`.
9. **Datos ficticios y sin secretos.** Ningún dato real ni secreto en el repositorio ni en los prompts. Las contraseñas se guardan con BCrypt. Los datos personales cumplen la Ley 1581 de 2012.
10. **Nada ambiguo pasa a código.** Lo que no esté claro se marca `[NECESITA ACLARACIÓN]` y se resuelve con el Product Owner antes de planificar.

## Gobernanza

- La constitución se aprueba en la **Puerta 0** y solo se enmienda con acuerdo de los dos Developers y visto bueno del Product Owner.
- Las enmiendas se registran al final de este archivo con fecha y motivo.
- Una puerta que nunca rechaza nada es un trámite: cada revisión debe dejar al menos un comentario por escrito.

## Enmiendas

_Ninguna todavía._
