package com.delivery.backoffice.admin.dto;

public record StaffUserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        String role,
        String restaurantId
) {
}
