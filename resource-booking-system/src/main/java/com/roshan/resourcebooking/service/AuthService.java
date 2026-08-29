package com.roshan.resourcebooking.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.roshan.resourcebooking.dto.LoginRequest;
import com.roshan.resourcebooking.dto.LoginResponse;
import com.roshan.resourcebooking.entity.User;
import com.roshan.resourcebooking.exception.UserNotFoundException;
import com.roshan.resourcebooking.repository.UserRepository;
import com.roshan.resourcebooking.security.JwtService;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final JwtService jwtService;

	public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.jwtService = jwtService;
	}

	public LoginResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password()));
		User user = userRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new UserNotFoundException("User not found"));
		String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
		return new LoginResponse(token);
	}
}
