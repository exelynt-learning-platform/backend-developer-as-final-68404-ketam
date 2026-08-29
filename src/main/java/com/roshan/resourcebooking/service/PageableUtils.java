package com.roshan.resourcebooking.service;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.roshan.resourcebooking.exception.InvalidSortException;

public final class PageableUtils {

	private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
			"id", "price", "startTime", "endTime", "createdAt", "updatedAt", "status");

	private PageableUtils() {
	}

	public static Pageable sanitize(Pageable pageable) {
		int page = Math.max(pageable.getPageNumber(), 0);
		int size = pageable.getPageSize() <= 0 ? 20 : Math.min(pageable.getPageSize(), 100);
		Sort sort = pageable.getSort();
		if (sort.isUnsorted()) {
			sort = Sort.by(Sort.Direction.DESC, "createdAt");
		}
		else {
			for (Sort.Order order : sort) {
				if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
					throw new InvalidSortException(
							"Invalid sort field: " + order.getProperty() + ". Allowed: " + ALLOWED_SORT_FIELDS);
				}
			}
		}
		return PageRequest.of(page, size, sort);
	}
}
