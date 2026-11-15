package com.retailstock.config;

import com.retailstock.repository.InventoryRepository;
import com.retailstock.repository.memory.InMemoryInventoryRepository;
import com.retailstock.service.InventoryService;

public class AppConfig {
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public AppConfig() {
        this.inventoryRepository = new InMemoryInventoryRepository();
        this.inventoryService = new InventoryService(inventoryRepository);
        DemoDataSeeder.seed(inventoryRepository);
    }

    public InventoryRepository inventoryRepository() { return inventoryRepository; }
    public InventoryService inventoryService() { return inventoryService; }
}
