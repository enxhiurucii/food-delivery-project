package com.delivery.webshop.cart.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.delivery.webshop.shared.error.BusinessRuleException;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Server-side cart, one per customer (id = token subject). A cart belongs to one restaurant at a time,
 * like in Wolt/Uber Eats. Abandoned carts expire automatically through a MongoDB TTL index.
 */
@Getter
@Document("carts")
public class Cart {

    public static final int MAX_QUANTITY_PER_ITEM = 50;

    @Id
    private String customerId;
    private String restaurantId;
    private List<CartItem> items = new ArrayList<>();
    @Indexed(expireAfter = "7d")
    private Instant updatedAt;

    protected Cart() {
    }

    public static Cart emptyFor(String customerId, Instant now) {
        Cart cart = new Cart();
        cart.customerId = customerId;
        cart.updatedAt = now;
        return cart;
    }

    public void addItem(String restaurantId, String menuItemId, String name, long unitPriceCents, int quantity,
                        Instant now) {
        requirePositive(quantity);
        if (!items.isEmpty() && !this.restaurantId.equals(restaurantId)) {
            throw new BusinessRuleException("CART_RESTAURANT_MISMATCH",
                    "Your cart contains items from another restaurant. Empty it first.");
        }
        this.restaurantId = restaurantId;
        Optional<CartItem> existing = find(menuItemId);
        if (existing.isPresent()) {
            replace(existing.get().withQuantity(cap(existing.get().quantity() + quantity)));
        } else {
            items.add(new CartItem(menuItemId, name, unitPriceCents, cap(quantity)));
        }
        updatedAt = now;
    }

    /** Quantity 0 removes the item. */
    public void changeQuantity(String menuItemId, int quantity, Instant now) {
        CartItem item = find(menuItemId).orElseThrow(() ->
                new BusinessRuleException("ITEM_NOT_IN_CART", "Item " + menuItemId + " is not in the cart"));
        if (quantity <= 0) {
            items.remove(item);
        } else {
            replace(item.withQuantity(cap(quantity)));
        }
        if (items.isEmpty()) {
            restaurantId = null;
        }
        updatedAt = now;
    }

    public void clear(Instant now) {
        items.clear();
        restaurantId = null;
        updatedAt = now;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public long subtotalCents() {
        return items.stream().mapToLong(CartItem::totalCents).sum();
    }

    private Optional<CartItem> find(String menuItemId) {
        return items.stream().filter(item -> item.menuItemId().equals(menuItemId)).findFirst();
    }

    private void replace(CartItem updated) {
        items.replaceAll(item -> item.menuItemId().equals(updated.menuItemId()) ? updated : item);
    }

    private static int cap(int quantity) {
        return Math.min(quantity, MAX_QUANTITY_PER_ITEM);
    }

    private static void requirePositive(int quantity) {
        if (quantity < 1) {
            throw new BusinessRuleException("INVALID_QUANTITY", "Quantity must be at least 1");
        }
    }
}
