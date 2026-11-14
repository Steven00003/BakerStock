package com.bakerstock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EsquemaColumnasEnumTest {

	@Autowired
	private JdbcTemplate jdbc;

	private void verificarColumna(String tabla, String columna) {
		String tipo = jdbc.queryForObject(
				"SELECT data_type FROM information_schema.columns "
						+ "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
				String.class, tabla, columna);
		Integer longitud = jdbc.queryForObject(
				"SELECT character_maximum_length FROM information_schema.columns "
						+ "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
				Integer.class, tabla, columna);
		assertThat(tipo).as("tipo de %s.%s", tabla, columna).isEqualTo("varchar");
		assertThat(longitud).as("longitud de %s.%s", tabla, columna).isEqualTo(40);
	}

	@Test
	@DisplayName("usuario.rol se crea como varchar(40) y no como ENUM nativo")
	void rolUsuarioVarchar() {
		verificarColumna("usuario", "rol");
	}

	@Test
	@DisplayName("evento_auditoria.accion se crea como varchar(40) y no como ENUM nativo")
	void accionAuditoriaVarchar() {
		verificarColumna("evento_auditoria", "accion");
	}

	@Test
	@DisplayName("evento_auditoria.resultado se crea como varchar(40) y no como ENUM nativo")
	void resultadoAuditoriaVarchar() {
		verificarColumna("evento_auditoria", "resultado");
	}

}
