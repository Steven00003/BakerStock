package com.bakerstock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BakerStockApplicationTests {

	@Test
	@DisplayName("AC-N003.1 el contexto arranca con el perfil test")
	void contextLoads() {
	}

}
