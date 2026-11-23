package com.delivery.backoffice.report.repository;

import java.time.Instant;
import java.util.List;

import com.delivery.backoffice.order.enums.OrderStatus;
import com.delivery.backoffice.order.model.Order;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

/**
 * Revenue breakdowns computed inside MongoDB with an aggregation pipeline:
 * <pre>
 * $match  { restaurantId, status in [PICKED_UP, DELIVERED], closedAt in [from, to) }
 * $group  { _id: "$fulfillmentType" | "$payment.method", orders: {$sum: 1}, totalCents: {$sum: "$totalCents"} }
 * $sort   { totalCents: -1 }
 * </pre>
 */
@Component
public class DailyReportQuery {

    public static final String BY_FULFILLMENT = "fulfillmentType";
    public static final String BY_PAYMENT_METHOD = "payment.method";

    private static final List<String> COMPLETED = List.of(OrderStatus.PICKED_UP.name(), OrderStatus.DELIVERED.name());

    private final MongoTemplate mongoTemplate;

    public DailyReportQuery(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<BreakdownRow> breakdown(String restaurantId, Instant from, Instant to, String groupByField) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("restaurantId").is(restaurantId)
                        .and("status").in(COMPLETED)
                        .and("closedAt").gte(from).lt(to)),
                Aggregation.group(groupByField).count().as("orders").sum("totalCents").as("totalCents"),
                Aggregation.sort(Sort.Direction.DESC, "totalCents"));
        return mongoTemplate.aggregate(aggregation, Order.class, BreakdownRow.class).getMappedResults();
    }

    /** {@code key} receives the group's {@code _id} (e.g. "DELIVERY" or "CASH"). */
    public record BreakdownRow(@Id String key, long orders, long totalCents) {
    }
}
