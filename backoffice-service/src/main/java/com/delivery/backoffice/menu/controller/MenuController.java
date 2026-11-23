package com.delivery.backoffice.menu.controller;

import java.util.List;

import com.delivery.backoffice.menu.dto.MenuItemRequest;
import com.delivery.backoffice.menu.dto.MenuItemResponse;
import com.delivery.backoffice.menu.dto.MenuRequest;
import com.delivery.backoffice.menu.dto.MenuResponse;
import com.delivery.backoffice.menu.service.MenuService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Menu editor. Everyone on the restaurant team can read; only managers can edit. */
@RestController
@RequestMapping("/api/restaurants/{restaurantId}/menus")
public class MenuController {

    private static final String CAN_READ = "@tenantGuard.canAccess(#restaurantId, authentication)";
    private static final String CAN_EDIT = "hasAnyRole('MANAGER','ADMIN') and @tenantGuard.canAccess(#restaurantId, authentication)";

    private final MenuService service;

    public MenuController(MenuService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(CAN_READ)
    public List<MenuResponse> getAllMenus(@PathVariable String restaurantId) {
        return service.getAllMenus(restaurantId).stream().map(MenuResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(CAN_EDIT)
    public MenuResponse createMenu(@PathVariable String restaurantId, @Valid @RequestBody MenuRequest request) {
        return MenuResponse.from(service.createMenu(restaurantId, request.toCommand()));
    }

    @PutMapping("/{menuId}")
    @PreAuthorize(CAN_EDIT)
    public MenuResponse updateMenu(@PathVariable String restaurantId, @PathVariable String menuId,
                        @Valid @RequestBody MenuRequest request) {
        return MenuResponse.from(service.updateMenu(restaurantId, menuId, request.toCommand()));
    }

    @DeleteMapping("/{menuId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(CAN_EDIT)
    public void deleteMenu(@PathVariable String restaurantId, @PathVariable String menuId) {
        service.deleteMenu(restaurantId, menuId);
    }

    @PostMapping("/{menuId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(CAN_EDIT)
    public MenuItemResponse addMenuItem(@PathVariable String restaurantId, @PathVariable String menuId,
                             @Valid @RequestBody MenuItemRequest request) {
        return MenuItemResponse.from(service.addMenuItem(restaurantId, menuId, request.toCommand()));
    }

    @PutMapping("/{menuId}/items/{itemId}")
    @PreAuthorize(CAN_EDIT)
    public MenuItemResponse updateMenuItem(@PathVariable String restaurantId, @PathVariable String menuId,
                                @PathVariable String itemId, @Valid @RequestBody MenuItemRequest request) {
        return MenuItemResponse.from(service.updateMenuItem(restaurantId, menuId, itemId, request.toCommand()));
    }

    @DeleteMapping("/{menuId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(CAN_EDIT)
    public void removeMenuItem(@PathVariable String restaurantId, @PathVariable String menuId, @PathVariable String itemId) {
        service.removeMenuItem(restaurantId, menuId, itemId);
    }
}
