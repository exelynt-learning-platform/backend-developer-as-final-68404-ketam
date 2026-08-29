package com.roshan.resourcebooking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncodingTest {

	private final PasswordEncoder encoder = new BCryptPasswordEncoder();

	@Test
	void bcryptEncodesAndVerifiesPassword() {
		String raw = "user@123";
		String encoded = encoder.encode(raw);

		assertThat(encoded).isNotEqualTo(raw);
		assertThat(encoded).startsWith("$2");
		assertThat(encoder.matches(raw, encoded)).isTrue();
		assertThat(encoder.matches("wrong", encoded)).isFalse();
	}
}
