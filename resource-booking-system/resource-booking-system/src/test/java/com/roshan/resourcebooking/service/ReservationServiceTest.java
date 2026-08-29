package com.roshan.resourcebooking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.roshan.resourcebooking.dto.PagedResponse;
import com.roshan.resourcebooking.dto.ReservationRequest;
import com.roshan.resourcebooking.dto.ReservationResponse;
import com.roshan.resourcebooking.entity.Reservation;
import com.roshan.resourcebooking.entity.ReservationStatus;
import com.roshan.resourcebooking.entity.Resource;
import com.roshan.resourcebooking.entity.Role;
import com.roshan.resourcebooking.entity.User;
import com.roshan.resourcebooking.exception.InvalidSortException;
import com.roshan.resourcebooking.exception.ReservationConflictException;
import com.roshan.resourcebooking.exception.ReservationNotFoundException;
import com.roshan.resourcebooking.repository.ReservationRepository;
import com.roshan.resourcebooking.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ResourceService resourceService;

	@Mock
	private UserRepository userRepository;

	private ReservationService reservationService;
	private CurrentUserService currentUserService;

	private final LocalDateTime start = LocalDateTime.of(2026, 9, 1, 10, 0);
	private final LocalDateTime end = LocalDateTime.of(2026, 9, 1, 12, 0);

	@BeforeEach
	void setUp() {
		currentUserService = new CurrentUserService(userRepository);
		reservationService = new ReservationService(reservationRepository, resourceService, currentUserService);
	}

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void userCanCreateReservationOwnedByAuthenticatedUser() {
		User user = authenticate("user", Role.USER, 5L);
		Resource resource = resource(1L, true);
		when(resourceService.getResource(1L)).thenReturn(resource);
		when(reservationRepository.existsOverlappingReservation(eq(1L), eq(start), eq(end), isNull())).thenReturn(false);
		when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
			Reservation reservation = invocation.getArgument(0);
			reservation.setId(10L);
			return reservation;
		});

		ReservationResponse response = reservationService.create(new ReservationRequest(1L, start, end, new BigDecimal("500.00")));

		assertThat(response.userId()).isEqualTo(5L);
		assertThat(response.username()).isEqualTo("user");
		assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
		ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
		verify(reservationRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isSameAs(user);
	}

	@Test
	void rejectsOverlappingReservation() {
		authenticate("user", Role.USER, 5L);
		when(resourceService.getResource(1L)).thenReturn(resource(1L, true));
		when(reservationRepository.existsOverlappingReservation(eq(1L), eq(start), eq(end), isNull())).thenReturn(true);

		assertThatThrownBy(() -> reservationService.create(new ReservationRequest(1L, start, end, new BigDecimal("100.00"))))
				.isInstanceOf(ReservationConflictException.class);
		verify(reservationRepository, never()).save(any());
	}

	@Test
	void rejectsInvalidTimeRange() {
		authenticate("user", Role.USER, 5L);
		assertThatThrownBy(() -> reservationService.create(
				new ReservationRequest(1L, end, start, new BigDecimal("10.00"))))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void userCannotViewAnotherUsersReservation() {
		authenticate("user", Role.USER, 5L);
		Reservation reservation = reservation(20L, user(9L, "other", Role.USER), resource(1L, true));
		when(reservationRepository.findById(20L)).thenReturn(Optional.of(reservation));

		assertThatThrownBy(() -> reservationService.findById(20L))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void userCanViewOwnReservation() {
		User user = authenticate("user", Role.USER, 5L);
		Reservation reservation = reservation(20L, user, resource(1L, true));
		when(reservationRepository.findById(20L)).thenReturn(Optional.of(reservation));

		ReservationResponse response = reservationService.findById(20L);
		assertThat(response.id()).isEqualTo(20L);
		assertThat(response.userId()).isEqualTo(5L);
	}

	@Test
	void adminCanViewAnyReservation() {
		authenticate("admin", Role.ADMIN, 1L);
		Reservation reservation = reservation(20L, user(9L, "other", Role.USER), resource(1L, true));
		when(reservationRepository.findById(20L)).thenReturn(Optional.of(reservation));

		assertThat(reservationService.findById(20L).username()).isEqualTo("other");
	}

	@Test
	void userSearchScopesToOwner() {
		authenticate("user", Role.USER, 5L);
		when(reservationRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

		reservationService.search(ReservationStatus.CONFIRMED, new BigDecimal("100"), new BigDecimal("1000"),
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "price")));

		verify(reservationRepository).findAll(any(Specification.class), any(Pageable.class));
	}

	@Test
	void adminSearchDoesNotForceOwner() {
		authenticate("admin", Role.ADMIN, 1L);
		when(reservationRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 4));

		PagedResponse<ReservationResponse> page = reservationService.search(null, null, null, PageRequest.of(0, 10));
		assertThat(page.totalElements()).isEqualTo(4);
		assertThat(page.page()).isZero();
		assertThat(page.size()).isEqualTo(10);
	}

	@Test
	void paginationAndSortingAreSanitized() {
		Pageable sanitized = PageableUtils.sanitize(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "startTime")));
		assertThat(sanitized.getPageNumber()).isZero();
		assertThat(sanitized.getPageSize()).isEqualTo(10);
		assertThat(sanitized.getSort().getOrderFor("startTime").getDirection()).isEqualTo(Sort.Direction.ASC);

		assertThatThrownBy(() -> PageableUtils.sanitize(PageRequest.of(0, 10, Sort.by("password"))))
				.isInstanceOf(InvalidSortException.class);
	}

	@Test
	void missingReservationThrowsNotFound() {
		authenticate("admin", Role.ADMIN, 1L);
		when(reservationRepository.findById(99L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> reservationService.findById(99L))
				.isInstanceOf(ReservationNotFoundException.class);
	}

	@Test
	void userCannotDeleteReservationInService() {
		authenticate("user", Role.USER, 5L);
		assertThatThrownBy(() -> reservationService.delete(1L))
				.isInstanceOf(AccessDeniedException.class);
	}

	private User authenticate(String username, Role role, Long id) {
		User user = user(id, username, role);
		lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				username,
				"n/a",
				List.of(new SimpleGrantedAuthority(role.authority())));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		return user;
	}

	private User user(Long id, String username, Role role) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		user.setRole(role);
		user.setPassword("encoded");
		return user;
	}

	private Resource resource(Long id, boolean available) {
		Resource resource = new Resource();
		resource.setId(id);
		resource.setName("Conference Room A");
		resource.setType("ROOM");
		resource.setAvailable(available);
		return resource;
	}

	private Reservation reservation(Long id, User user, Resource resource) {
		Reservation reservation = new Reservation();
		reservation.setId(id);
		reservation.setUser(user);
		reservation.setResource(resource);
		reservation.setStartTime(start);
		reservation.setEndTime(end);
		reservation.setPrice(new BigDecimal("500.00"));
		reservation.setStatus(ReservationStatus.PENDING);
		return reservation;
	}
}
