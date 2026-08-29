package com.roshan.resourcebooking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.roshan.resourcebooking.dto.ResourceRequest;
import com.roshan.resourcebooking.dto.ResourceResponse;
import com.roshan.resourcebooking.entity.Resource;
import com.roshan.resourcebooking.exception.ResourceNotFoundException;
import com.roshan.resourcebooking.repository.ResourceRepository;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

	@Mock
	private ResourceRepository resourceRepository;

	private ResourceService resourceService;

	@BeforeEach
	void setUp() {
		resourceService = new ResourceService(resourceRepository);
	}

	@Test
	void createPersistsResource() {
		ResourceRequest request = new ResourceRequest("Conference Room A", "Large meeting room", "ROOM", true);
		when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> {
			Resource resource = invocation.getArgument(0);
			resource.setId(1L);
			return resource;
		});

		ResourceResponse response = resourceService.create(request);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.name()).isEqualTo("Conference Room A");
		assertThat(response.available()).isTrue();
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(resourceRepository.findById(99L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> resourceService.findById(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void updateAndDeleteUseExistingEntity() {
		Resource resource = new Resource();
		resource.setId(2L);
		resource.setName("Old");
		resource.setType("ROOM");
		resource.setAvailable(true);
		when(resourceRepository.findById(2L)).thenReturn(Optional.of(resource));
		when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ResourceResponse updated = resourceService.update(2L, new ResourceRequest("New", "d", "LAB", false));
		assertThat(updated.name()).isEqualTo("New");
		assertThat(updated.available()).isFalse();

		resourceService.delete(2L);
		ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
		verify(resourceRepository).delete(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(2L);
	}

	@Test
	void findAllMapsEntities() {
		Resource resource = new Resource();
		resource.setId(1L);
		resource.setName("Room");
		resource.setType("ROOM");
		resource.setAvailable(true);
		when(resourceRepository.findAll()).thenReturn(List.of(resource));
		assertThat(resourceService.findAll()).hasSize(1);
	}
}
