package com.roshan.resourcebooking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.roshan.resourcebooking.dto.ResourceRequest;
import com.roshan.resourcebooking.dto.ResourceResponse;
import com.roshan.resourcebooking.entity.Resource;
import com.roshan.resourcebooking.exception.ResourceNotFoundException;
import com.roshan.resourcebooking.repository.ResourceRepository;

@Service
@Transactional
public class ResourceService {

	private final ResourceRepository resourceRepository;

	public ResourceService(ResourceRepository resourceRepository) {
		this.resourceRepository = resourceRepository;
	}

	public ResourceResponse create(ResourceRequest request) {
		Resource resource = new Resource();
		apply(resource, request);
		return toResponse(resourceRepository.save(resource));
	}

	@Transactional(readOnly = true)
	public List<ResourceResponse> findAll() {
		return resourceRepository.findAll().stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public ResourceResponse findById(Long id) {
		return toResponse(getResource(id));
	}

	public ResourceResponse update(Long id, ResourceRequest request) {
		Resource resource = getResource(id);
		apply(resource, request);
		return toResponse(resourceRepository.save(resource));
	}

	public void delete(Long id) {
		Resource resource = getResource(id);
		resourceRepository.delete(resource);
	}

	public Resource getResource(Long id) {
		return resourceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Resource not found with id " + id));
	}

	private void apply(Resource resource, ResourceRequest request) {
		resource.setName(request.name());
		resource.setDescription(request.description());
		resource.setType(request.type());
		resource.setAvailable(Boolean.TRUE.equals(request.available()));
	}

	public ResourceResponse toResponse(Resource resource) {
		return new ResourceResponse(
				resource.getId(),
				resource.getName(),
				resource.getDescription(),
				resource.getType(),
				resource.isAvailable(),
				resource.getCreatedAt(),
				resource.getUpdatedAt());
	}
}
