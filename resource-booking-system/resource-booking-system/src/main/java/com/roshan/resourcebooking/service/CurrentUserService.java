package com.roshan.resourcebooking.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.roshan.resourcebooking.entity.Role;
import com.roshan.resourcebooking.entity.User;
import com.roshan.resourcebooking.exception.UserNotFoundException;
import com.roshan.resourcebooking.repository.UserRepository;

@Service
public class CurrentUserService {

	private final UserRepository userRepository;

	public CurrentUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User requireCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
			throw new AccessDeniedException("Unauthenticated");
		}
		return userRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new UserNotFoundException("Authenticated user was not found"));
	}

	public boolean isAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch(authority -> Role.ADMIN.authority().equals(authority));
	}
}
