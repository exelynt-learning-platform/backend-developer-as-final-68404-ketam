package com.roshan.resourcebooking.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.roshan.resourcebooking.dto.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final JsonMapper jsonMapper;

	public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService, JsonMapper jsonMapper) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.jsonMapper = jsonMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = header.substring(7).trim();
		if (token.isEmpty() || !jwtService.isValid(token)) {
			writeUnauthorized(request, response);
			return;
		}

		try {
			String username = jwtService.extractUsername(token);
			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails,
						null,
						userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			filterChain.doFilter(request, response);
		}
		catch (Exception ex) {
			SecurityContextHolder.clearContext();
			writeUnauthorized(request, response);
		}
	}

	private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (response.isCommitted()) {
			return;
		}
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				HttpServletResponse.SC_UNAUTHORIZED,
				"Unauthorized",
				"Invalid or expired JWT",
				request.getRequestURI());
		jsonMapper.writeValue(response.getOutputStream(), body);
	}
}
