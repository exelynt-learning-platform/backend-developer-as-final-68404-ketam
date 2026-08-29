package com.roshan.resourcebooking.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService("resource-booking-local-dev-secret-key-must-be-at-least-256-bits-long", 3600000L);
	}

	@Test
	void generatesAndValidatesTokenWithUsernameAndRole() {
		String token = jwtService.generateToken("user", "USER");

		assertThat(jwtService.isValid(token)).isTrue();
		assertThat(jwtService.extractUsername(token)).isEqualTo("user");
		assertThat(jwtService.extractRole(token)).isEqualTo("USER");
	}

	@Test
	void rejectsTamperedToken() {
		String token = jwtService.generateToken("user", "USER") + "x";
		assertThat(jwtService.isValid(token)).isFalse();
	}

	@Test
	void rejectsExpiredToken() {
		JwtService shortLived = new JwtService(
				"resource-booking-local-dev-secret-key-must-be-at-least-256-bits-long",
				-1000L);
		String token = shortLived.generateToken("user", "USER");
		assertThat(shortLived.isValid(token)).isFalse();
		assertThatThrownBy(() -> shortLived.extractUsername(token))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
