package com.retailstock.service;

import com.retailstock.dto.ProductRequest;
import com.retailstock.dto.SaleRequest;
import com.retailstock.dto.TransactionRequest;
import com.retailstock.exception.ApiException;
import com.retailstock.exception.DuplicateSkuException;
import com.retailstock.exception.InsufficientStockException;
import com.retailstock.model.Product;
import com.retailstock.model.Purchase;
import com.retailstock.model.PurchaseItem;
import com.retailstock.model.Sale;
import com.retailstock.model.SaleItem;
import com.retailstock.repository.InventoryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class InventoryService {
    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) { this.repository = repository; }

    public List<Product> getProducts(String search) {
        List<Product> products = repository.findAllActiveProducts();
        if (search == null || search.isBlank()) return products;
        String term = search.trim().toLowerCase();
        return products.stream().filter(p -> p.getName().toLowerCase().contains(term)
                || p.getSku().toLowerCase().contains(term)
                || p.getCategory().toLowerCase().contains(term)).collect(Collectors.toList());
    }

    public Product getProduct(long id) {
        return repository.findProductById(id).orElseThrow(() -> new ApiException(404, "Product not found."));
    }

    public Product createProduct(ProductRequest request) {
        validateProductRequest(request, true);
        String sku = clean(request.sku).toUpperCase();
        if (repository.findProductBySku(sku).isPresent()) throw new DuplicateSkuException("SKU '" + sku + "' already exists.");
        Product product = new Product();
        product.setSku(sku);
        product.setName(clean(request.name));
        product.setCategory(clean(request.category));
        product.setSellingPrice(money(request.sellingPrice));
        product.setAverageCost(money(request.averageCost == null ? BigDecimal.ZERO : request.averageCost));
        product.setCurrentStock(request.currentStock == null ? 0 : request.currentStock);
        product.setReorderLevel(request.reorderLevel == null ? 0 : request.reorderLevel);
        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now().toString());
        product.setUpdatedAt(product.getCreatedAt());
        return repository.saveProduct(product);
    }

    public Product updateProduct(long id, ProductRequest request) {
        validateProductRequest(request, false);
        Product existing = getProduct(id);
        String sku = clean(request.sku).toUpperCase();
        repository.findProductBySku(sku).filter(p -> p.getId() != id).ifPresent(p -> {
            throw new DuplicateSkuException("SKU '" + sku + "' already exists.");
        });
        existing.setSku(sku);
        existing.setName(clean(request.name));
        existing.setCategory(clean(request.category));
        existing.setSellingPrice(money(request.sellingPrice));
        if (request.averageCost != null) existing.setAverageCost(money(request.averageCost));
        if (request.reorderLevel != null) existing.setReorderLevel(request.reorderLevel);
        existing.setUpdatedAt(LocalDateTime.now().toString());
        return repository.saveProduct(existing);
    }

    public void deactivateProduct(long id) {
        getProduct(id);
        if (!repository.deactivateProduct(id)) throw new ApiException(404, "Product not found.");
    }

    public List<Product> getLowStockProducts() {
        return repository.findAllActiveProducts().stream()
                .filter(p -> p.getCurrentStock() <= p.getReorderLevel())
                .sorted(Comparator.comparingInt(Product::getCurrentStock)).collect(Collectors.toList());
    }

    public synchronized Purchase recordPurchase(TransactionRequest request) {
        if (request == null || request.items == null || request.items.isEmpty())
            throw new ApiException(400, "Add at least one item to the purchase.");
        if (request.supplierName == null || request.supplierName.isBlank())
            throw new ApiException(400, "Supplier name is required.");

        Purchase purchase = new Purchase();
        purchase.setReceiptNumber("PUR-" + System.currentTimeMillis());
        purchase.setSupplierName(clean(request.supplierName));
        purchase.setPurchaseDate(LocalDateTime.now().toString());
        List<PurchaseItem> items = new ArrayList<>();
        Map<Long, Product> updatedProducts = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (TransactionRequest.LineItemRequest line : request.items) {
            if (line == null || line.productId == null || line.quantity == null || line.unitCost == null)
                throw new ApiException(400, "Each purchase item needs a product, quantity and unit cost.");
            if (line.quantity <= 0) throw new ApiException(400, "Purchase quantity must be greater than zero.");
            if (line.unitCost.signum() < 0) throw new ApiException(400, "Unit cost cannot be negative.");
            Product product = updatedProducts.containsKey(line.productId) ? updatedProducts.get(line.productId) : getProduct(line.productId);
            BigDecimal oldValue = product.getAverageCost().multiply(BigDecimal.valueOf(product.getCurrentStock()));
            BigDecimal newValue = line.unitCost.multiply(BigDecimal.valueOf(line.quantity));
            int newStock = product.getCurrentStock() + line.quantity;
            BigDecimal average = newStock == 0 ? BigDecimal.ZERO : oldValue.add(newValue)
                    .divide(BigDecimal.valueOf(newStock), 2, RoundingMode.HALF_UP);
            product.setCurrentStock(newStock);
            product.setAverageCost(average);
            product.setUpdatedAt(LocalDateTime.now().toString());
            updatedProducts.put(product.getId(), product);

            PurchaseItem item = new PurchaseItem();
            item.setProductId(product.getId()); item.setProductName(product.getName()); item.setSku(product.getSku());
            item.setQuantity(line.quantity); item.setUnitCost(money(line.unitCost)); item.setLineTotal(money(newValue));
            items.add(item);
            total = total.add(newValue);
        }
        purchase.setItems(items);
        purchase.setTotalAmount(money(total));
        return repository.savePurchaseWithProducts(purchase, new ArrayList<>(updatedProducts.values()));
    }

    public synchronized Sale recordSale(SaleRequest request) {
        if (request == null || request.items == null || request.items.isEmpty())
            throw new ApiException(400, "Add at least one item to the sale.");

        Map<Long, Integer> requestedQuantities = new HashMap<>();
        for (SaleRequest.LineItem line : request.items) {
            if (line == null || line.productId == null || line.quantity == null)
                throw new ApiException(400, "Each sale item needs a product and quantity.");
            if (line.quantity <= 0) throw new ApiException(400, "Sale quantity must be greater than zero.");
            requestedQuantities.merge(line.productId, line.quantity, Integer::sum);
        }

        Map<Long, Product> products = new LinkedHashMap<>();
        requestedQuantities.forEach((productId, quantity) -> {
            Product product = getProduct(productId);
            if (product.getCurrentStock() < quantity) {
                throw new InsufficientStockException("Insufficient stock for " + product.getName()
                        + ". Available: " + product.getCurrentStock() + ", requested: " + quantity + ".");
            }
            products.put(productId, product);
        });

        Sale sale = new Sale();
        sale.setInvoiceNumber("INV-" + System.currentTimeMillis());
        sale.setSaleDate(LocalDateTime.now().toString());
        List<SaleItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            Product product = products.get(entry.getKey());
            int quantity = entry.getValue();
            BigDecimal lineTotal = product.getSellingPrice().multiply(BigDecimal.valueOf(quantity));
            SaleItem item = new SaleItem();
            item.setProductId(product.getId()); item.setProductName(product.getName()); item.setSku(product.getSku());
            item.setQuantity(quantity); item.setUnitPrice(product.getSellingPrice()); item.setLineTotal(money(lineTotal));
            items.add(item);
            total = total.add(lineTotal);
            product.setCurrentStock(product.getCurrentStock() - quantity);
            product.setUpdatedAt(LocalDateTime.now().toString());
        }
        sale.setItems(items);
        sale.setTotalAmount(money(total));
        return repository.saveSaleWithProducts(sale, new ArrayList<>(products.values()));
    }

    public List<Purchase> getPurchases() {
        return repository.findAllPurchases().stream()
                .sorted(Comparator.comparing(Purchase::getPurchaseDate).reversed()).collect(Collectors.toList());
    }
    public List<Sale> getSales() {
        return repository.findAllSales().stream()
                .sorted(Comparator.comparing(Sale::getSaleDate).reversed()).collect(Collectors.toList());
    }

    public Map<String, Object> dashboard() {
        List<Product> products = repository.findAllActiveProducts();
        List<Sale> sales = repository.findAllSales();
        BigDecimal stockValue = products.stream().map(p -> p.getAverageCost().multiply(BigDecimal.valueOf(p.getCurrentStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal retailValue = products.stream().map(p -> p.getSellingPrice().multiply(BigDecimal.valueOf(p.getCurrentStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate today = LocalDate.now();
        BigDecimal todaySales = sales.stream().filter(s -> LocalDate.parse(s.getSaleDate().substring(0, 10)).equals(today))
                .map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalUnits = products.stream().mapToInt(Product::getCurrentStock).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productCount", products.size()); result.put("totalUnits", totalUnits);
        result.put("stockValue", money(stockValue)); result.put("retailValue", money(retailValue));
        result.put("lowStockCount", products.stream().filter(p -> p.getCurrentStock() <= p.getReorderLevel()).count());
        result.put("todaySales", money(todaySales)); result.put("salesCount", sales.size());
        result.put("recentSales", sales.stream().sorted(Comparator.comparing(Sale::getSaleDate).reversed()).limit(5).collect(Collectors.toList()));
        result.put("lowStockProducts", getLowStockProducts().stream().limit(5).collect(Collectors.toList()));
        return result;
    }

    public List<Map<String, Object>> valuationReport() {
        Map<String, List<Product>> byCategory = repository.findAllActiveProducts().stream()
                .collect(Collectors.groupingBy(Product::getCategory, LinkedHashMap::new, Collectors.toList()));
        return byCategory.entrySet().stream().map(entry -> {
            List<Product> products = entry.getValue();
            int units = products.stream().mapToInt(Product::getCurrentStock).sum();
            BigDecimal cost = products.stream().map(p -> p.getAverageCost().multiply(BigDecimal.valueOf(p.getCurrentStock())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal retail = products.stream().map(p -> p.getSellingPrice().multiply(BigDecimal.valueOf(p.getCurrentStock())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", entry.getKey()); row.put("productCount", products.size()); row.put("units", units);
            row.put("stockValue", money(cost)); row.put("retailValue", money(retail));
            return row;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> fastMovingReport() {
        Map<Long, Integer> sold = repository.findAllSales().stream().flatMap(s -> s.getItems().stream())
                .collect(Collectors.groupingBy(SaleItem::getProductId, Collectors.summingInt(SaleItem::getQuantity)));
        Map<Long, Product> productMap = repository.findAllActiveProducts().stream().collect(Collectors.toMap(Product::getId, p -> p));
        return sold.entrySet().stream().filter(e -> productMap.containsKey(e.getKey()))
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()).map(e -> {
                    Product p = productMap.get(e.getKey());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", p.getId()); row.put("sku", p.getSku()); row.put("name", p.getName());
                    row.put("category", p.getCategory()); row.put("unitsSold", e.getValue());
                    row.put("remainingStock", p.getCurrentStock());
                    return row;
                }).collect(Collectors.toList());
    }

    public Map<String, Object> salesReport(String from, String to) {
        List<Sale> filtered = repository.findAllSales().stream().filter(s -> {
            String day = s.getSaleDate().substring(0, 10);
            return (from == null || from.isBlank() || day.compareTo(from) >= 0)
                    && (to == null || to.isBlank() || day.compareTo(to) <= 0);
        }).collect(Collectors.toList());
        BigDecimal total = filtered.stream().map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transactionCount", filtered.size()); result.put("totalSales", money(total));
        result.put("averageSale", filtered.isEmpty() ? BigDecimal.ZERO : money(total.divide(BigDecimal.valueOf(filtered.size()), 2, RoundingMode.HALF_UP)));
        result.put("transactions", filtered.stream().sorted(Comparator.comparing(Sale::getSaleDate).reversed()).collect(Collectors.toList()));
        return result;
    }

    private void validateProductRequest(ProductRequest request, boolean creating) {
        if (request == null) throw new ApiException(400, "Request body is required.");
        if (isBlank(request.sku) || isBlank(request.name) || isBlank(request.category))
            throw new ApiException(400, "SKU, product name and category are required.");
        if (request.sellingPrice == null || request.sellingPrice.signum() <= 0)
            throw new ApiException(400, "Selling price must be greater than zero.");
        if (request.averageCost != null && request.averageCost.signum() < 0)
            throw new ApiException(400, "Average cost cannot be negative.");
        if (request.reorderLevel != null && request.reorderLevel < 0)
            throw new ApiException(400, "Reorder level cannot be negative.");
        if (creating && request.currentStock != null && request.currentStock < 0)
            throw new ApiException(400, "Opening stock cannot be negative.");
    }

    private static boolean isBlank(String value) { return value == null || value.isBlank(); }
    private static String clean(String value) { return Objects.requireNonNull(value).trim(); }
    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
