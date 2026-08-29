package com.roshan.resourcebooking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.roshan.resourcebooking.entity.ReservationStatus;

public record ReservationResponse(
		Long id,
		Long userId,
		String username,
		Long resourceId,
		String resourceName,
		LocalDateTime startTime,
		LocalDateTime endTime,
		BigDecimal price,
		ReservationStatus status,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {
}
