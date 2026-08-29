package com.roshan.resourcebooking.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.roshan.resourcebooking.dto.PagedResponse;
import com.roshan.resourcebooking.dto.ReservationRequest;
import com.roshan.resourcebooking.dto.ReservationResponse;
import com.roshan.resourcebooking.dto.ReservationUpdateRequest;
import com.roshan.resourcebooking.entity.Reservation;
import com.roshan.resourcebooking.entity.ReservationStatus;
import com.roshan.resourcebooking.entity.Resource;
import com.roshan.resourcebooking.entity.User;
import com.roshan.resourcebooking.exception.ReservationConflictException;
import com.roshan.resourcebooking.exception.ReservationNotFoundException;
import com.roshan.resourcebooking.repository.ReservationRepository;
import com.roshan.resourcebooking.repository.ReservationSpecifications;

@Service
@Transactional
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final ResourceService resourceService;
	private final CurrentUserService currentUserService;

	public ReservationService(
			ReservationRepository reservationRepository,
			ResourceService resourceService,
			CurrentUserService currentUserService) {
		this.reservationRepository = reservationRepository;
		this.resourceService = resourceService;
		this.currentUserService = currentUserService;
	}

	public ReservationResponse create(ReservationRequest request) {
		User currentUser = currentUserService.requireCurrentUser();
		validateTimeRange(request.startTime(), request.endTime());
		Resource resource = resourceService.getResource(request.resourceId());
		if (!resource.isAvailable()) {
			throw new ReservationConflictException("Resource is not available for booking");
		}
		assertNoOverlap(resource.getId(), request.startTime(), request.endTime(), null);

		Reservation reservation = new Reservation();
		reservation.setUser(currentUser);
		reservation.setResource(resource);
		reservation.setStartTime(request.startTime());
		reservation.setEndTime(request.endTime());
		reservation.setPrice(request.price());
		reservation.setStatus(ReservationStatus.PENDING);
		return toResponse(reservationRepository.save(reservation));
	}

	@Transactional(readOnly = true)
	public PagedResponse<ReservationResponse> search(
			ReservationStatus status,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Pageable pageable) {
		User currentUser = currentUserService.requireCurrentUser();
		boolean admin = currentUserService.isAdmin();
		Long ownerId = admin ? null : currentUser.getId();
		Pageable safePageable = PageableUtils.sanitize(pageable);
		Page<Reservation> page = reservationRepository.findAll(
				ReservationSpecifications.withFilters(ownerId, status, minPrice, maxPrice),
				safePageable);
		return new PagedResponse<>(
				page.getContent().stream().map(this::toResponse).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}

	@Transactional(readOnly = true)
	public ReservationResponse findById(Long id) {
		Reservation reservation = getReservation(id);
		assertCanView(reservation);
		return toResponse(reservation);
	}

	public ReservationResponse update(Long id, ReservationUpdateRequest request) {
		assertAdmin();
		validateTimeRange(request.startTime(), request.endTime());
		Reservation reservation = getReservation(id);
		Resource resource = resourceService.getResource(request.resourceId());
		assertNoOverlap(resource.getId(), request.startTime(), request.endTime(), reservation.getId());
		reservation.setResource(resource);
		reservation.setStartTime(request.startTime());
		reservation.setEndTime(request.endTime());
		reservation.setPrice(request.price());
		reservation.setStatus(request.status());
		return toResponse(reservationRepository.save(reservation));
	}

	public void delete(Long id) {
		assertAdmin();
		Reservation reservation = getReservation(id);
		reservationRepository.delete(reservation);
	}

	private void assertCanView(Reservation reservation) {
		User currentUser = currentUserService.requireCurrentUser();
		if (currentUserService.isAdmin()) {
			return;
		}
		if (!reservation.getUser().getId().equals(currentUser.getId())) {
			throw new AccessDeniedException("You are not allowed to access this reservation");
		}
	}

	private void assertAdmin() {
		if (!currentUserService.isAdmin()) {
			throw new AccessDeniedException("Admin role is required");
		}
	}

	private Reservation getReservation(Long id) {
		return reservationRepository.findById(id)
				.orElseThrow(() -> new ReservationNotFoundException("Reservation not found with id " + id));
	}

	private void assertNoOverlap(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
		if (reservationRepository.existsOverlappingReservation(resourceId, startTime, endTime, excludeId)) {
			throw new ReservationConflictException("The resource is already reserved for the selected time range");
		}
	}

	private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
		if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
			throw new IllegalArgumentException("endTime must be after startTime");
		}
	}

	private ReservationResponse toResponse(Reservation reservation) {
		return new ReservationResponse(
				reservation.getId(),
				reservation.getUser().getId(),
				reservation.getUser().getUsername(),
				reservation.getResource().getId(),
				reservation.getResource().getName(),
				reservation.getStartTime(),
				reservation.getEndTime(),
				reservation.getPrice(),
				reservation.getStatus(),
				reservation.getCreatedAt(),
				reservation.getUpdatedAt());
	}
}
