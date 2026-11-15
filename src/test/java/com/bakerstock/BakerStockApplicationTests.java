package com.bakerstock;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BakerStockApplicationTests {

	@Autowired
	private Environment environment;

	@Test
	@DisplayName("AC-N003.1 el contexto arranca con el perfil test")
	void contextLoads() {
		assertThat(environment).isNotNull();
	}

	@Test
	@DisplayName("AC-N003.1 el timeout de sesión vale 30 minutos")
	void sessionTimeoutEs30Minutos() {
		Duration timeout = environment.getProperty("server.servlet.session.timeout", Duration.class);
		assertThat(timeout).isEqualTo(Duration.ofMinutes(30));
	}

}
