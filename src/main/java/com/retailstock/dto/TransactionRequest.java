package com.retailstock.dto;

import java.math.BigDecimal;
import java.util.List;

public class TransactionRequest {
    public String supplierName;
    public List<LineItemRequest> items;

    public static class LineItemRequest {
        public Long productId;
        public Integer quantity;
        public BigDecimal unitCost;
    }
}
