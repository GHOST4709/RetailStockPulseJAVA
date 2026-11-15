package com.retailstock.config;

import com.retailstock.model.Product;
import com.retailstock.repository.InventoryRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Loads a small starter catalogue so the dashboard is useful on first launch. */
public final class DemoDataSeeder {
    private DemoDataSeeder() { }

    public static void seed(InventoryRepository repository) {
        add(repository, "ELC-1001", "Wireless Mouse", "Electronics", "799.00", "420.00", 34, 10);
        add(repository, "ELC-1002", "Mechanical Keyboard", "Electronics", "2499.00", "1650.00", 8, 10);
        add(repository, "OFF-2001", "A5 Notebook", "Stationery", "65.00", "32.00", 120, 30);
        add(repository, "OFF-2002", "Gel Pen (Blue)", "Stationery", "25.00", "11.00", 16, 20);
        add(repository, "GRC-3001", "Premium Tea 250g", "Grocery", "185.00", "132.00", 42, 12);
        add(repository, "GRC-3002", "Arabica Coffee 200g", "Grocery", "349.00", "245.00", 5, 8);
        add(repository, "HOM-4001", "Steel Water Bottle", "Home & Living", "499.00", "290.00", 22, 8);
        add(repository, "HOM-4002", "Desk Organizer", "Home & Living", "299.00", "155.00", 0, 6);
    }

    private static void add(InventoryRepository repository, String sku, String name, String category,
                            String price, String cost, int stock, int reorder) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setCategory(category);
        product.setSellingPrice(new BigDecimal(price));
        product.setAverageCost(new BigDecimal(cost));
        product.setCurrentStock(stock);
        product.setReorderLevel(reorder);
        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now().toString());
        product.setUpdatedAt(product.getCreatedAt());
        repository.saveProduct(product);
    }
}
