package com.roshan.resourcebooking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.roshan.resourcebooking.entity.ReservationStatus;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ReservationUpdateRequest(
		@NotNull(message = "Resource ID is required")
		Long resourceId,

		@NotNull(message = "Start time is required")
		LocalDateTime startTime,

		@NotNull(message = "End time is required")
		LocalDateTime endTime,

		@NotNull(message = "Price is required")
		@PositiveOrZero(message = "Price must be greater than or equal to 0")
		@Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 fraction digits")
		BigDecimal price,

		@NotNull(message = "Status is required")
		ReservationStatus status) {

	@AssertTrue(message = "endTime must be after startTime")
	public boolean isValidTimeRange() {
		if (startTime == null || endTime == null) {
			return true;
		}
		return endTime.isAfter(startTime);
	}
}
