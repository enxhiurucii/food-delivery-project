package com.delivery.backoffice.staff.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignStaffRequest(@NotBlank String subject, @NotBlank String displayName) {
}
