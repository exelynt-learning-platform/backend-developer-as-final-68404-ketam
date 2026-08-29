package com.roshan.resourcebooking.dto;

import java.time.LocalDateTime;

public record ResourceResponse(
		Long id,
		String name,
		String description,
		String type,
		boolean available,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {
}
