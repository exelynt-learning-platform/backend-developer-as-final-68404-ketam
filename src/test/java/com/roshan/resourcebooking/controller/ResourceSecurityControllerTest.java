package com.roshan.resourcebooking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.roshan.resourcebooking.dto.LoginResponse;
import com.roshan.resourcebooking.dto.ResourceResponse;
import com.roshan.resourcebooking.exception.GlobalExceptionHandler;
import com.roshan.resourcebooking.security.JwtService;
import com.roshan.resourcebooking.security.RestAccessDeniedHandler;
import com.roshan.resourcebooking.security.RestAuthenticationEntryPoint;
import com.roshan.resourcebooking.security.SecurityConfig;
import com.roshan.resourcebooking.service.AuthService;
import com.roshan.resourcebooking.service.CustomUserDetailsService;
import com.roshan.resourcebooking.service.ResourceService;

@WebMvcTest(controllers = {AuthController.class, ResourceController.class})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtService.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
		GlobalExceptionHandler.class})
class ResourceSecurityControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private ResourceService resourceService;

	@MockitoBean
	private AuthService authService;

	@Test
	void loginSucceeds() throws Exception {
		when(authService.login(any())).thenReturn(new LoginResponse("jwt-token"));
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"user","password":"user@123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("jwt-token"));
	}

	@Test
	void loginFailsWithInvalidCredentials() throws Exception {
		when(authService.login(any())).thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"user","password":"wrong"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void jwtAuthenticatesUserForResourceRead() throws Exception {
		String token = jwtService.generateToken("user", "USER");
		when(customUserDetailsService.loadUserByUsername("user")).thenReturn(User.withUsername("user")
				.password("encoded")
				.roles("USER")
				.build());
		when(resourceService.findAll()).thenReturn(List.of());

		mockMvc.perform(get("/resources").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void expiredOrInvalidJwtIsUnauthorized() throws Exception {
		mockMvc.perform(get("/resources").header("Authorization", "Bearer not-a-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "user", roles = "USER")
	void userCanReadResources() throws Exception {
		when(resourceService.findById(1L)).thenReturn(new ResourceResponse(1L, "Room", "d", "ROOM", true, null, null));
		mockMvc.perform(get("/resources")).andExpect(status().isOk());
		mockMvc.perform(get("/resources/1")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "user", roles = "USER")
	void userCannotCreateResources() throws Exception {
		mockMvc.perform(post("/resources")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Conference Room A","description":"Large meeting room","type":"ROOM","available":true}
								"""))
				.andExpect(status().isForbidden());
		verify(resourceService, never()).create(any());
	}

	@Test
	@WithMockUser(username = "user", roles = "USER")
	void userCannotUpdateResources() throws Exception {
		mockMvc.perform(put("/resources/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Conference Room A","description":"Large meeting room","type":"ROOM","available":true}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "user", roles = "USER")
	void userCannotDeleteResources() throws Exception {
		mockMvc.perform(delete("/resources/1")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminCanCreateUpdateAndDeleteResources() throws Exception {
		when(resourceService.create(any())).thenReturn(new ResourceResponse(1L, "Conference Room A", "Large meeting room", "ROOM", true, null, null));
		when(resourceService.update(any(), any())).thenReturn(new ResourceResponse(1L, "Updated", "d", "ROOM", true, null, null));

		mockMvc.perform(post("/resources")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Conference Room A","description":"Large meeting room","type":"ROOM","available":true}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(put("/resources/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Updated","description":"d","type":"ROOM","available":true}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(delete("/resources/1")).andExpect(status().isNoContent());
	}
}
