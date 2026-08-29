package com.roshan.resourcebooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.roshan.resourcebooking.entity.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
