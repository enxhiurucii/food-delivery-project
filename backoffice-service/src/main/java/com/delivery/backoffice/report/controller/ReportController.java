package com.delivery.backoffice.report.controller;

import java.time.LocalDate;

import com.delivery.backoffice.order.dto.OrderResponse;
import com.delivery.backoffice.order.model.Order;
import com.delivery.backoffice.report.dto.PageResponse;
import com.delivery.backoffice.report.service.ReportService;
import com.delivery.backoffice.report.service.ReportService.DailyReport;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Daily report and receipts (closed orders). Managers only. */
@Validated
@RestController
@RequestMapping("/api/restaurants/{restaurantId}")
public class ReportController {

    private static final String MANAGERS = "hasAnyRole('MANAGER','ADMIN') and @tenantGuard.canAccess(#restaurantId, authentication)";

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    /** {@code GET /api/restaurants/{id}/reports/daily?date=2026-11-23} */
    @GetMapping("/reports/daily")
    @PreAuthorize(MANAGERS)
    DailyReport getDailyReport(@PathVariable String restaurantId,
                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reports.getDailyReport(restaurantId, date);
    }

    /** {@code GET /api/restaurants/{id}/receipts?date=2026-11-23&page=0&size=20} */
    @GetMapping("/receipts")
    @PreAuthorize(MANAGERS)
    PageResponse<OrderResponse> getReceipts(@PathVariable String restaurantId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                         @RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<Order> result = reports.getReceipts(restaurantId, date, page, size);
        return new PageResponse<>(result.getContent().stream().map(OrderResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
