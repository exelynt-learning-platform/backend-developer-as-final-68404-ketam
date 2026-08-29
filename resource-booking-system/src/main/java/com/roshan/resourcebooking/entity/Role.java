package com.roshan.resourcebooking.entity;

public enum Role {
	ADMIN,
	USER;

	public String authority() {
		return "ROLE_" + name();
	}
}
