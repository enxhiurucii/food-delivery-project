package com.delivery.backoffice.staff.dto;

import java.util.Set;

public record MeResponse(String subject, String username, String name, Set<String> roles, String restaurantId) {
}
