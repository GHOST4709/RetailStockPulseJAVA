package com.retailstock.controller;

import com.retailstock.dto.ProductRequest;
import com.retailstock.dto.SaleRequest;
import com.retailstock.dto.TransactionRequest;
import com.retailstock.service.InventoryService;

/** Thin HTTP-facing controller: validation and business rules live in InventoryService. */
public class ApiController {
    private final InventoryService service;
    public ApiController(InventoryService service) { this.service = service; }
    public InventoryService service() { return service; }
    public Object createProduct(ProductRequest request) { return service.createProduct(request); }
    public Object updateProduct(long id, ProductRequest request) { return service.updateProduct(id, request); }
    public Object recordPurchase(TransactionRequest request) { return service.recordPurchase(request); }
    public Object recordSale(SaleRequest request) { return service.recordSale(request); }
}
