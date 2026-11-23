package com.delivery.backoffice.admin.controller;

import com.delivery.backoffice.admin.dto.CreateDriverRequest;
import com.delivery.backoffice.admin.dto.CreateManagerRequest;
import com.delivery.backoffice.admin.dto.StaffUserResponse;
import com.delivery.backoffice.admin.service.UserManagementService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserManagementService service;

    public UserManagementController(UserManagementService service) {
        this.service = service;
    }

    @PostMapping("/managers")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffUserResponse createManager(@Valid @RequestBody CreateManagerRequest request) {
        return service.createManager(request.restaurantId(), request.username(), request.email(),
                request.firstName(), request.lastName(), request.password());
    }

    @PostMapping("/drivers")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffUserResponse createDriver(@Valid @RequestBody CreateDriverRequest request) {
        return service.createDriver(request.username(), request.email(),
                request.firstName(), request.lastName(), request.password());
    }
}
