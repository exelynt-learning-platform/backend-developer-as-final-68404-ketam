package com.roshan.resourcebooking.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.roshan.resourcebooking.dto.PagedResponse;
import com.roshan.resourcebooking.dto.ReservationRequest;
import com.roshan.resourcebooking.dto.ReservationResponse;
import com.roshan.resourcebooking.dto.ReservationUpdateRequest;
import com.roshan.resourcebooking.entity.ReservationStatus;
import com.roshan.resourcebooking.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/reservations")
@Tag(name = "Reservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a reservation for the authenticated user")
	public ReservationResponse create(@Valid @RequestBody ReservationRequest request) {
		return reservationService.create(request);
	}

	@GetMapping
	@Operation(summary = "List reservations with optional filters, pagination, and sorting")
	public PagedResponse<ReservationResponse> findAll(
			@RequestParam(required = false) ReservationStatus status,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@PageableDefault(size = 20) Pageable pageable) {
		return reservationService.search(status, minPrice, maxPrice, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a reservation by id")
	public ReservationResponse findById(@PathVariable Long id) {
		return reservationService.findById(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a reservation (ADMIN)")
	public ReservationResponse update(@PathVariable Long id, @Valid @RequestBody ReservationUpdateRequest request) {
		return reservationService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a reservation (ADMIN)")
	public void delete(@PathVariable Long id) {
		reservationService.delete(id);
	}
}
