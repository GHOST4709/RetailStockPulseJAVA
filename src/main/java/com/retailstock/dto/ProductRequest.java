package com.retailstock.dto;

import java.math.BigDecimal;

public class ProductRequest {
    public String sku;
    public String name;
    public String category;
    public BigDecimal sellingPrice;
    public BigDecimal averageCost;
    public Integer currentStock;
    public Integer reorderLevel;
}
