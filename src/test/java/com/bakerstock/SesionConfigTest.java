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
class SesionConfigTest {

	@Autowired
	private Environment environment;

	@Test
	@DisplayName("AC-N003.1 la sesión expira tras 30 minutos de inactividad")
	void sessionTimeoutEs30Minutos() {
		Duration timeout = environment.getProperty("server.servlet.session.timeout", Duration.class);
		assertThat(timeout).isEqualTo(Duration.ofMinutes(30));
	}

}
