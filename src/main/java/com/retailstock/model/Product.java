package com.retailstock.model;

import java.math.BigDecimal;

public class Product {
    private long id;
    private String sku;
    private String name;
    private String category;
    private BigDecimal sellingPrice;
    private BigDecimal averageCost;
    private int currentStock;
    private int reorderLevel;
    private boolean active = true;
    private String createdAt;
    private String updatedAt;

    public Product copy() {
        Product copy = new Product();
        copy.id = id; copy.sku = sku; copy.name = name; copy.category = category;
        copy.sellingPrice = sellingPrice; copy.averageCost = averageCost;
        copy.currentStock = currentStock; copy.reorderLevel = reorderLevel;
        copy.active = active; copy.createdAt = createdAt; copy.updatedAt = updatedAt;
        return copy;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public BigDecimal getAverageCost() { return averageCost; }
    public void setAverageCost(BigDecimal averageCost) { this.averageCost = averageCost; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
