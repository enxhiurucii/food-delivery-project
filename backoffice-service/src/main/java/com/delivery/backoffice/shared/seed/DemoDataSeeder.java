package com.delivery.backoffice.shared.seed;

import java.time.Clock;
import java.time.Instant;

import com.delivery.backoffice.menu.model.Menu;
import com.delivery.backoffice.menu.repository.MenuRepository;
import com.delivery.backoffice.restaurant.model.Restaurant;
import com.delivery.backoffice.restaurant.model.RestaurantDetails;
import com.delivery.backoffice.restaurant.repository.RestaurantRepository;
import com.delivery.backoffice.staff.model.StaffMember;
import com.delivery.backoffice.staff.repository.StaffMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Demo data so a reviewer can log in and order within seconds. The staff subjects below are the fixed
 * user ids declared in delivery-platform-infra/keycloak/staff-realm.json.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String NAPOLI_ID = "napoli";
    public static final String SUSHI_ID = "sushi-zen";
    static final String NAPOLI_MANAGER = "22222222-2222-2222-2222-222222222222";
    static final String NAPOLI_STAFF = "33333333-3333-3333-3333-333333333333";
    static final String SUSHI_MANAGER = "44444444-4444-4444-4444-444444444444";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final RestaurantRepository restaurants;
    private final MenuRepository menus;
    private final StaffMemberRepository staff;
    private final Clock clock;

    public DemoDataSeeder(RestaurantRepository restaurants, MenuRepository menus, StaffMemberRepository staff,
                          Clock clock) {
        this.restaurants = restaurants;
        this.menus = menus;
        this.staff = staff;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (restaurants.count() > 0) {
            return;
        }
        Instant now = clock.instant();

        restaurants.save(Restaurant.create(NAPOLI_ID, new RestaurantDetails(
                "Pizzeria Napoli", "Wood-fired pizza and burgers", "Rruga e Durrësit 12, Tirana", "+355 69 000 0001",
                "https://picsum.photos/seed/napoli/800/400", "Europe/Tirane",
                true, true, true, 250, 1000, 20), now));

        Menu lunch = Menu.create(NAPOLI_ID, "Lunch menu", "Served 11:00-15:00", 1, now);
        lunch.addItem("Burger menu", "Beef burger, fries and Coca-Cola", 1000, "https://picsum.photos/seed/burger/400/300", true, now);
        lunch.addItem("Pizza menu", "Margherita and a soft drink", 1100, "https://picsum.photos/seed/pizza/400/300", true, now);
        Menu pizzas = Menu.create(NAPOLI_ID, "Pizzas", null, 2, now);
        pizzas.addItem("Margherita", "Tomato, mozzarella, basil", 800, "https://picsum.photos/seed/margherita/400/300", true, now);
        pizzas.addItem("Diavola", "Spicy salami", 950, "https://picsum.photos/seed/diavola/400/300", true, now);
        Menu drinks = Menu.create(NAPOLI_ID, "Drinks", null, 3, now);
        drinks.addItem("Coca-Cola 0.33l", null, 250, "https://picsum.photos/seed/cola/400/300", true, now);
        drinks.addItem("Water 0.5l", null, 150, "https://picsum.photos/seed/water/400/300", true, now);
        menus.save(lunch);
        menus.save(pizzas);
        menus.save(drinks);

        restaurants.save(Restaurant.create(SUSHI_ID, new RestaurantDetails(
                "Sushi Zen", "Sushi and bowls", "Blloku, Tirana", "+355 69 000 0002",
                "https://picsum.photos/seed/sushi/800/400", "Europe/Tirane",
                true, true, false, 0, 1500, 25), now));
        Menu sushi = Menu.create(SUSHI_ID, "Sushi", null, 1, now);
        sushi.addItem("Salmon maki (8)", null, 900, "https://picsum.photos/seed/maki/400/300", true, now);
        sushi.addItem("Poke bowl", "Salmon, rice, avocado", 1300, "https://picsum.photos/seed/poke/400/300", true, now);
        menus.save(sushi);

        staff.save(new StaffMember(NAPOLI_MANAGER, NAPOLI_ID, "Napoli Manager", now));
        staff.save(new StaffMember(NAPOLI_STAFF, NAPOLI_ID, "Napoli Kitchen", now));
        staff.save(new StaffMember(SUSHI_MANAGER, SUSHI_ID, "Sushi Manager", now));

        log.info("Seeded demo restaurants '{}' and '{}'", NAPOLI_ID, SUSHI_ID);
    }
}
