package com.delivery.backoffice.report.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.delivery.backoffice.order.model.Order;

import org.springframework.data.domain.Page;

public interface ReportService {

    DailyReport getDailyReport(String restaurantId, LocalDate date);

    Page<Order> getReceipts(String restaurantId, LocalDate date, int page, int size);

    record DailyReport(LocalDate date, String timezone, long orders, BigDecimal revenue,
                       BigDecimal averageOrderValue, String currency,
                       List<Breakdown> byFulfillmentType, List<Breakdown> byPaymentMethod) {
    }

    record Breakdown(String key, long orders, BigDecimal revenue) {
    }
}
