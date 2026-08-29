package com.roshan.resourcebooking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.roshan.resourcebooking.dto.PagedResponse;
import com.roshan.resourcebooking.dto.ReservationResponse;
import com.roshan.resourcebooking.entity.ReservationStatus;
import com.roshan.resourcebooking.exception.GlobalExceptionHandler;
import com.roshan.resourcebooking.security.JwtService;
import com.roshan.resourcebooking.security.RestAccessDeniedHandler;
import com.roshan.resourcebooking.security.RestAuthenticationEntryPoint;
import com.roshan.resourcebooking.security.SecurityConfig;
import com.roshan.resourcebooking.service.CustomUserDetailsService;
import com.roshan.resourcebooking.service.ReservationService;

@WebMvcTest(controllers = ReservationController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtService.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
		GlobalExceptionHandler.class})
class ReservationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private ReservationService reservationService;

	@Test
	@WithMockUser(username = "user", roles = "USER")
	void userCanCreateReservation() throws Exception {
		when(reservationService.create(any())).thenReturn(sample(5L, "user"));
		mockMvc.perform(post("/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resourceId":1,"startTime":"2026-09-01T10:00:00","endTime":"2026-09-01T12:00:00","price":500.00}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(5));
	}

	@Test
	@WithMockUser(username = "user", roles = "USER")
	void invalidPriceIsRejected() throws Exception {
		mockMvc.perform(post("/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resourceId":1,"startTime":"2026-09-01T10:00:00","endTime":"2026-09-01T12:00:00","price":-1}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(username = "user", roles = "USER")
	void invalidTimeRangeIsRejected() throws Exception {
		mockMvc.perform(post("/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resourceId":1,"startTime":"2026-09-01T12:00:00","endTime":"2026-09-01T10:00:00","price":10.00}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void invalidStatusIsRejected() throws Exception {
		mockMvc.perform(put("/reservations/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resourceId":1,"startTime":"2026-09-01T10:00:00","endTime":"2026-09-01T12:00:00","price":10.00,"status":"UNKNOWN"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(username = "user", roles = "USER")
	void statusMinAndMaxPriceFiltersArePassedThrough() throws Exception {
		when(reservationService.search(eq(ReservationStatus.CONFIRMED), eq(new BigDecimal("100")), eq(new BigDecimal("1000")), any(Pageable.class)))
				.thenReturn(new PagedResponse<>(List.of(sample(5L, "user")), 0, 10, 1, 1));

		mockMvc.perform(get("/reservations")
						.param("status", "CONFIRMED")
						.param("minPrice", "100")
						.param("maxPrice", "1000")
						.param("page", "0")
						.param("size", "10")
						.param("sort", "price,desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));
	}

	@Test
	@WithMockUser(username = "user", roles = "USER")
	void invalidStatusQueryIsBadRequest() throws Exception {
		mockMvc.perform(get("/reservations").param("status", "NOPE"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(username = "user", roles = "USER")
	void userCannotUpdateReservationAtEndpoint() throws Exception {
		mockMvc.perform(put("/reservations/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"resourceId":1,"startTime":"2026-09-01T10:00:00","endTime":"2026-09-01T12:00:00","price":10.00,"status":"CONFIRMED"}
								"""))
				.andExpect(status().isForbidden());
	}

	private ReservationResponse sample(Long userId, String username) {
		return new ReservationResponse(
				10L,
				userId,
				username,
				1L,
				"Conference Room A",
				LocalDateTime.of(2026, 9, 1, 10, 0),
				LocalDateTime.of(2026, 9, 1, 12, 0),
				new BigDecimal("500.00"),
				ReservationStatus.PENDING,
				null,
				null);
	}
}
