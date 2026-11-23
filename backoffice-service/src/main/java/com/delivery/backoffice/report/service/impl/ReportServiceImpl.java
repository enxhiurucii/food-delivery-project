package com.delivery.backoffice.report.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

import com.delivery.backoffice.order.enums.OrderStatus;
import com.delivery.backoffice.order.model.Order;
import com.delivery.backoffice.order.repository.OrderRepository;
import com.delivery.backoffice.report.repository.DailyReportQuery;
import com.delivery.backoffice.report.repository.DailyReportQuery.BreakdownRow;
import com.delivery.backoffice.report.service.ReportService;
import com.delivery.backoffice.restaurant.service.RestaurantService;
import com.delivery.backoffice.shared.money.Money;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Daily figures. A "day" is computed in the RESTAURANT's timezone, not UTC: an order closed at 23:30 in
 * Tirana belongs to that day even though it is already the next day in some other zone.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final EnumSet<OrderStatus> COMPLETED = EnumSet.of(OrderStatus.PICKED_UP, OrderStatus.DELIVERED);

    private final RestaurantService restaurants;
    private final DailyReportQuery query;
    private final OrderRepository orders;

    public ReportServiceImpl(RestaurantService restaurants, DailyReportQuery query, OrderRepository orders) {
        this.restaurants = restaurants;
        this.query = query;
        this.orders = orders;
    }

    @Override
    public DailyReport getDailyReport(String restaurantId, LocalDate date) {
        DayWindow window = window(restaurantId, date);
        List<Breakdown> byFulfillment = toBreakdown(
                query.breakdown(restaurantId, window.from(), window.to(), DailyReportQuery.BY_FULFILLMENT));
        List<Breakdown> byPayment = toBreakdown(
                query.breakdown(restaurantId, window.from(), window.to(), DailyReportQuery.BY_PAYMENT_METHOD));

        long orderCount = byFulfillment.stream().mapToLong(Breakdown::orders).sum();
        BigDecimal revenue = byFulfillment.stream().map(Breakdown::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = orderCount == 0
                ? BigDecimal.ZERO.setScale(2)
                : revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
        return new DailyReport(date, window.zone().getId(), orderCount, revenue, average, Money.CURRENCY,
                byFulfillment, byPayment);
    }

    @Override
    public Page<Order> getReceipts(String restaurantId, LocalDate date, int page, int size) {
        DayWindow window = window(restaurantId, date);
        return orders.findByRestaurantIdAndStatusInAndClosedAtBetween(restaurantId, COMPLETED, window.from(),
                window.to(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "closedAt")));
    }

    private DayWindow window(String restaurantId, LocalDate date) {
        ZoneId zone = restaurants.getRestaurantById(restaurantId).zone();
        return new DayWindow(zone, date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant());
    }

    private static List<Breakdown> toBreakdown(List<BreakdownRow> rows) {
        return rows.stream().map(r -> new Breakdown(r.key(), r.orders(), Money.toDecimal(r.totalCents()))).toList();
    }

    private record DayWindow(ZoneId zone, Instant from, Instant to) {
    }
}
