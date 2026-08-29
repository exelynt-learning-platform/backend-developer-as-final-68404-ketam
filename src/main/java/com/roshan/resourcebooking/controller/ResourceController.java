package com.roshan.resourcebooking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.roshan.resourcebooking.dto.ResourceRequest;
import com.roshan.resourcebooking.dto.ResourceResponse;
import com.roshan.resourcebooking.service.ResourceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/resources")
@Tag(name = "Resources")
public class ResourceController {

	private final ResourceService resourceService;

	public ResourceController(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a resource (ADMIN)")
	public ResourceResponse create(@Valid @RequestBody ResourceRequest request) {
		return resourceService.create(request);
	}

	@GetMapping
	@Operation(summary = "List resources")
	public List<ResourceResponse> findAll() {
		return resourceService.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a resource by id")
	public ResourceResponse findById(@PathVariable Long id) {
		return resourceService.findById(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a resource (ADMIN)")
	public ResourceResponse update(@PathVariable Long id, @Valid @RequestBody ResourceRequest request) {
		return resourceService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a resource (ADMIN)")
	public void delete(@PathVariable Long id) {
		resourceService.delete(id);
	}
}
