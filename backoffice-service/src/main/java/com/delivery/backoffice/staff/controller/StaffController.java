package com.delivery.backoffice.staff.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.delivery.backoffice.shared.security.TenantGuard;
import com.delivery.backoffice.staff.dto.AssignStaffRequest;
import com.delivery.backoffice.staff.dto.MeResponse;
import com.delivery.backoffice.staff.model.StaffMember;
import com.delivery.backoffice.staff.service.StaffService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaffController {

    private final StaffService staffService;
    private final TenantGuard tenantGuard;

    public StaffController(StaffService staffService, TenantGuard tenantGuard) {
        this.staffService = staffService;
        this.tenantGuard = tenantGuard;
    }

    /** First call the back-office UI makes after login: who am I and which restaurant do I work at? */
    @GetMapping("/api/me")
    public MeResponse getCurrentStaffMember(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .collect(Collectors.toSet());
        String restaurantId = tenantGuard.staffMemberOf(authentication).map(StaffMember::getRestaurantId).orElse(null);
        return new MeResponse(jwt.getSubject(), jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("name"), roles, restaurantId);
    }

    @GetMapping("/api/restaurants/{restaurantId}/staff")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN') and @tenantGuard.canAccess(#restaurantId, authentication)")
    public List<StaffMember> getStaffByRestaurant(@PathVariable String restaurantId) {
        return staffService.getStaffByRestaurant(restaurantId);
    }

    /** Links a Keycloak user (by subject id) to a restaurant. */
    @PostMapping("/api/restaurants/{restaurantId}/staff")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public StaffMember assignStaffMember(@PathVariable String restaurantId, @Valid @RequestBody AssignStaffRequest request) {
        return staffService.assignStaffMember(restaurantId, request.subject(), request.displayName());
    }
}
