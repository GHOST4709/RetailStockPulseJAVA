package com.retailstock.dto;

import java.util.List;

public class SaleRequest {
    public List<LineItem> items;

    public static class LineItem {
        public Long productId;
        public Integer quantity;
    }
}
