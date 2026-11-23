package com.delivery.backoffice.catalog.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.delivery.backoffice.catalog.dto.CatalogItem;
import com.delivery.backoffice.catalog.dto.CatalogMenu;
import com.delivery.backoffice.catalog.dto.CatalogRestaurant;
import com.delivery.backoffice.catalog.dto.PricedLine;
import com.delivery.backoffice.catalog.dto.PricingRequest;
import com.delivery.backoffice.catalog.dto.PricingResponse;
import com.delivery.backoffice.catalog.dto.RequestedLine;
import com.delivery.backoffice.catalog.service.CatalogService;
import com.delivery.backoffice.menu.model.Menu;
import com.delivery.backoffice.menu.model.MenuItem;
import com.delivery.backoffice.menu.repository.MenuRepository;
import com.delivery.backoffice.restaurant.model.Restaurant;
import com.delivery.backoffice.restaurant.service.RestaurantService;
import com.delivery.backoffice.shared.error.BusinessRuleException;

import org.springframework.stereotype.Service;

/** Read-only projection of restaurants and menus for the customer-facing webshop. */
@Service
public class CatalogServiceImpl implements CatalogService {

    private final RestaurantService restaurants;
    private final MenuRepository menus;

    public CatalogServiceImpl(RestaurantService restaurants, MenuRepository menus) {
        this.restaurants = restaurants;
        this.menus = menus;
    }

    @Override
    public List<CatalogRestaurant> getAllRestaurants() {
        return restaurants.getAllRestaurants().stream().map(CatalogServiceImpl::toCatalog).toList();
    }

    @Override
    public CatalogRestaurant getRestaurantById(String restaurantId) {
        return toCatalog(restaurants.getRestaurantById(restaurantId));
    }

    @Override
    public List<CatalogMenu> getMenusByRestaurant(String restaurantId) {
        restaurants.getRestaurantById(restaurantId);
        return menus.findByRestaurantIdOrderByPositionAsc(restaurantId).stream()
                .map(menu -> new CatalogMenu(menu.getId(), menu.getName(), menu.getDescription(),
                        menu.getItems().stream().map(CatalogServiceImpl::toCatalog).toList()))
                .toList();
    }

    /**
     * Validates every requested item (exists in this restaurant, currently available) and returns the
     * current price. The webshop applies the ordering rules (open, fulfillment type, minimum order).
     */
    @Override
    public PricingResponse calculatePrice(String restaurantId, PricingRequest request) {
        Restaurant restaurant = restaurants.getRestaurantById(restaurantId);
        Map<String, MenuItem> itemsById = menus.findByRestaurantIdOrderByPositionAsc(restaurantId).stream()
                .map(Menu::getItems)
                .flatMap(List::stream)
                // first-wins merge: a menu item id is unique within a restaurant by invariant
                .collect(Collectors.toMap(MenuItem::id, Function.identity(), (a, b) -> a));

        List<PricedLine> lines = request.lines().stream()
                .map(line -> priceLine(itemsById, line))
                .toList();

        return new PricingResponse(restaurant.getId(), restaurant.getName(), restaurant.isAcceptingOrders(),
                restaurant.isPickupEnabled(), restaurant.isDeliveryEnabled(), restaurant.getDeliveryFeeCents(),
                restaurant.getMinimumOrderCents(), restaurant.getEstimatedPrepMinutes(), lines);
    }

    private static PricedLine priceLine(Map<String, MenuItem> itemsById, RequestedLine line) {
        MenuItem item = itemsById.get(line.menuItemId());
        if (item == null) {
            throw new BusinessRuleException("ITEM_NOT_FOUND", "Menu item " + line.menuItemId() + " does not exist");
        }
        if (!item.available()) {
            throw new BusinessRuleException("ITEM_UNAVAILABLE", item.name() + " is currently not available");
        }
        return new PricedLine(item.id(), item.name(), item.priceCents(), line.quantity());
    }

    private static CatalogRestaurant toCatalog(Restaurant r) {
        return new CatalogRestaurant(r.getId(), r.getName(), r.getDescription(), r.getAddress(), r.getImageUrl(),
                r.isAcceptingOrders(), r.isPickupEnabled(), r.isDeliveryEnabled(), r.getDeliveryFeeCents(),
                r.getMinimumOrderCents(), r.getEstimatedPrepMinutes());
    }

    private static CatalogItem toCatalog(MenuItem item) {
        return new CatalogItem(item.id(), item.name(), item.description(), item.priceCents(), item.imageUrl(),
                item.available());
    }
}
