package com.roshan.resourcebooking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.roshan.resourcebooking.dto.LoginRequest;
import com.roshan.resourcebooking.dto.LoginResponse;
import com.roshan.resourcebooking.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	@SecurityRequirements
	@Operation(summary = "Authenticate and receive a JWT")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}
}
