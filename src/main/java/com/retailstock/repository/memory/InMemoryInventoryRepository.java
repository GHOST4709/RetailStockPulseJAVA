package com.retailstock.repository.memory;

import com.retailstock.model.Product;
import com.retailstock.model.Purchase;
import com.retailstock.model.Sale;
import com.retailstock.repository.InventoryRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryInventoryRepository implements InventoryRepository {
    private final Map<Long, Product> products = new LinkedHashMap<>();
    private final Map<Long, Purchase> purchases = new LinkedHashMap<>();
    private final Map<Long, Sale> sales = new LinkedHashMap<>();
    private final AtomicLong productIds = new AtomicLong(1);
    private final AtomicLong purchaseIds = new AtomicLong(1);
    private final AtomicLong saleIds = new AtomicLong(1);

    @Override public synchronized List<Product> findAllActiveProducts() {
        return products.values().stream().filter(Product::isActive).map(Product::copy).collect(Collectors.toList());
    }
    @Override public synchronized Optional<Product> findProductById(long id) {
        Product product = products.get(id);
        return product == null || !product.isActive() ? Optional.empty() : Optional.of(product.copy());
    }
    @Override public synchronized Optional<Product> findProductBySku(String sku) {
        return products.values().stream().filter(Product::isActive)
                .filter(p -> p.getSku().equalsIgnoreCase(sku)).findFirst().map(Product::copy);
    }
    @Override public synchronized Product saveProduct(Product product) {
        Product copy = product.copy();
        if (copy.getId() <= 0) copy.setId(productIds.getAndIncrement());
        else productIds.updateAndGet(current -> Math.max(current, copy.getId() + 1));
        products.put(copy.getId(), copy);
        product.setId(copy.getId());
        return copy.copy();
    }
    @Override public synchronized boolean deactivateProduct(long id) {
        Product product = products.get(id);
        if (product == null || !product.isActive()) return false;
        product.setActive(false);
        return true;
    }
    @Override public synchronized Purchase savePurchaseWithProducts(Purchase purchase, List<Product> updatedProducts) {
        purchase.setId(purchaseIds.getAndIncrement());
        purchases.put(purchase.getId(), purchase);
        updatedProducts.forEach(this::saveProduct);
        return purchase;
    }
    @Override public synchronized List<Purchase> findAllPurchases() {
        return new ArrayList<>(purchases.values());
    }
    @Override public synchronized Sale saveSaleWithProducts(Sale sale, List<Product> updatedProducts) {
        sale.setId(saleIds.getAndIncrement());
        sales.put(sale.getId(), sale);
        updatedProducts.forEach(this::saveProduct);
        return sale;
    }
    @Override public synchronized List<Sale> findAllSales() {
        return new ArrayList<>(sales.values());
    }
}
