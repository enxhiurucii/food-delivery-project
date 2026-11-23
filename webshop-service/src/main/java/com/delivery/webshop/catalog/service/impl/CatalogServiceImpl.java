package com.delivery.webshop.catalog.service.impl;

import java.util.List;
import java.util.function.Supplier;

import com.delivery.webshop.catalog.client.BackofficeClient;
import com.delivery.webshop.catalog.dto.CatalogMenu;
import com.delivery.webshop.catalog.dto.CatalogRestaurant;
import com.delivery.webshop.catalog.dto.PricingRequest;
import com.delivery.webshop.catalog.dto.PricingResponse;
import com.delivery.webshop.catalog.service.CatalogService;
import com.delivery.webshop.shared.error.BackofficeUnavailableException;
import com.delivery.webshop.shared.error.BusinessRuleException;
import com.delivery.webshop.shared.error.NotFoundException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Anti-corruption layer around the HTTP client: translates remote failures into the webshop's own
 * exceptions and caches catalog reads (30 s, Caffeine). Prices for checkout are never cached.
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    private final BackofficeClient client;

    public CatalogServiceImpl(BackofficeClient client) {
        this.client = client;
    }

    @Override
    @Cacheable("catalog-restaurants")
    public List<CatalogRestaurant> getAllRestaurants() {
        return call(client::getAllRestaurants, "restaurants");
    }

    @Override
    @Cacheable("catalog-restaurant")
    public CatalogRestaurant getRestaurantById(String restaurantId) {
        return call(() -> client.getRestaurantById(restaurantId), "restaurant " + restaurantId);
    }

    @Override
    @Cacheable("catalog-menus")
    public List<CatalogMenu> getMenusByRestaurant(String restaurantId) {
        return call(() -> client.getMenusByRestaurant(restaurantId), "menus of " + restaurantId);
    }

    @Override
    public PricingResponse calculatePrice(String restaurantId, PricingRequest request) {
        return call(() -> client.calculatePrice(restaurantId, request), "prices of " + restaurantId);
    }

    private static <T> T call(Supplier<T> remoteCall, String what) {
        try {
            return remoteCall.get();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Restaurant data", what);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 422) {
                ProblemDetail problem = ex.getResponseBodyAs(ProblemDetail.class);
                String code = problem != null && problem.getProperties() != null
                        ? String.valueOf(problem.getProperties().getOrDefault("code", "INVALID_ORDER"))
                        : "INVALID_ORDER";
                String detail = problem != null ? problem.getDetail() : ex.getMessage();
                throw new BusinessRuleException(code, detail);
            }
            // 401/403 here means OUR service identity is misconfigured: not the customer's fault.
            throw new BackofficeUnavailableException("Back office rejected the call for " + what, ex);
        } catch (RestClientException ex) {
            throw new BackofficeUnavailableException("Back office unavailable for " + what, ex);
        }
    }
}
