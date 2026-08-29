package com.roshan.resourcebooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.roshan.resourcebooking.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

	@Query("""
			SELECT COUNT(r) > 0
			FROM Reservation r
			WHERE r.resource.id = :resourceId
			  AND r.status <> com.roshan.resourcebooking.entity.ReservationStatus.CANCELLED
			  AND (:excludeId IS NULL OR r.id <> :excludeId)
			  AND r.startTime < :endTime
			  AND r.endTime > :startTime
			""")
	boolean existsOverlappingReservation(
			@Param("resourceId") Long resourceId,
			@Param("startTime") java.time.LocalDateTime startTime,
			@Param("endTime") java.time.LocalDateTime endTime,
			@Param("excludeId") Long excludeId);
}
