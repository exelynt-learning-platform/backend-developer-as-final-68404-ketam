package com.roshan.resourcebooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResourceRequest(
		@NotBlank(message = "Name is required")
		@Size(max = 150, message = "Name must be at most 150 characters")
		String name,

		@Size(max = 500, message = "Description must be at most 500 characters")
		String description,

		@NotBlank(message = "Type is required")
		@Size(max = 100, message = "Type must be at most 100 characters")
		String type,

		@NotNull(message = "Available is required")
		Boolean available) {
}
