package com.roshan.resourcebooking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.roshan.resourcebooking.dto.LoginRequest;
import com.roshan.resourcebooking.dto.LoginResponse;
import com.roshan.resourcebooking.entity.Role;
import com.roshan.resourcebooking.entity.User;
import com.roshan.resourcebooking.repository.UserRepository;
import com.roshan.resourcebooking.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserRepository userRepository;

	@Mock
	private JwtService jwtService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(authenticationManager, userRepository, jwtService);
	}

	@Test
	void loginReturnsJwtOnSuccess() {
		LoginRequest request = new LoginRequest("user", "user@123");
		Authentication authentication = new UsernamePasswordAuthenticationToken("user", "user@123");
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

		User user = new User();
		user.setUsername("user");
		user.setRole(Role.USER);
		when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
		when(jwtService.generateToken("user", "USER")).thenReturn("jwt-token");

		LoginResponse response = authService.login(request);

		assertThat(response.token()).isEqualTo("jwt-token");
		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
	}

	@Test
	void loginPropagatesInvalidCredentials() {
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> authService.login(new LoginRequest("user", "wrong")))
				.isInstanceOf(BadCredentialsException.class);
	}
}
