package com.roshan.resourcebooking.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.roshan.resourcebooking.entity.Reservation;
import com.roshan.resourcebooking.entity.ReservationStatus;

import jakarta.persistence.criteria.Predicate;

public final class ReservationSpecifications {

	private ReservationSpecifications() {
	}

	public static Specification<Reservation> withFilters(
			Long ownerUserId,
			ReservationStatus status,
			BigDecimal minPrice,
			BigDecimal maxPrice) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (ownerUserId != null) {
				predicates.add(cb.equal(root.get("user").get("id"), ownerUserId));
			}
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			if (minPrice != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
			}
			if (maxPrice != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}
}
