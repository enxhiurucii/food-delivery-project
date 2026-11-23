package com.delivery.backoffice.menu.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.delivery.backoffice.shared.error.NotFoundException;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A menu section such as "Lunch menu" or "Drinks". Items are embedded: they are always read and edited
 * together with their menu and the list is small, which is the textbook case for embedding in MongoDB.
 */
@Getter
@Document("menus")
public class Menu {

    @Id
    private String id;
    @Indexed
    private String restaurantId;
    private String name;
    private String description;
    private int position;
    private List<MenuItem> items = new ArrayList<>();
    private Instant updatedAt;
    @Version
    private Long version;

    protected Menu() {
    }

    public static Menu create(String restaurantId, String name, String description, int position, Instant now) {
        Menu menu = new Menu();
        menu.restaurantId = restaurantId;
        menu.name = name;
        menu.description = description;
        menu.position = position;
        menu.updatedAt = now;
        return menu;
    }

    public void rename(String name, String description, int position, Instant now) {
        this.name = name;
        this.description = description;
        this.position = position;
        this.updatedAt = now;
    }

    public MenuItem addItem(String name, String description, long priceCents, String imageUrl, boolean available,
                            Instant now) {
        MenuItem item = new MenuItem(UUID.randomUUID().toString(), name, description, priceCents, imageUrl, available);
        items.add(item);
        updatedAt = now;
        return item;
    }

    public MenuItem updateItem(String itemId, String name, String description, long priceCents, String imageUrl,
                               boolean available, Instant now) {
        int index = indexOf(itemId);
        MenuItem updated = new MenuItem(itemId, name, description, priceCents, imageUrl, available);
        items.set(index, updated);
        updatedAt = now;
        return updated;
    }

    public void removeItem(String itemId, Instant now) {
        items.remove(indexOf(itemId));
        updatedAt = now;
    }

    public Optional<MenuItem> findItem(String itemId) {
        return items.stream().filter(item -> item.id().equals(itemId)).findFirst();
    }

    private int indexOf(String itemId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id().equals(itemId)) {
                return i;
            }
        }
        throw new NotFoundException("Menu item", itemId);
    }
}
