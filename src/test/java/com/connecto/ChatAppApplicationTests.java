package com.connecto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ChatAppApplicationTests {

	@Test
	void contextLoads() {
		assertDoesNotThrow(() -> Class.forName("com.connecto.ChatAppApplication"));
	}

}
