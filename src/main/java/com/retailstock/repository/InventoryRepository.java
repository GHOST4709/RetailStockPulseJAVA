package com.retailstock.repository;

import com.retailstock.model.Product;
import com.retailstock.model.Purchase;
import com.retailstock.model.Sale;

import java.util.List;
import java.util.Optional;

/** Storage contract. The JDBC implementation can replace the in-memory adapter later. */
public interface InventoryRepository {
    List<Product> findAllActiveProducts();
    Optional<Product> findProductById(long id);
    Optional<Product> findProductBySku(String sku);
    Product saveProduct(Product product);
    boolean deactivateProduct(long id);
    Purchase savePurchaseWithProducts(Purchase purchase, List<Product> updatedProducts);
    List<Purchase> findAllPurchases();
    Sale saveSaleWithProducts(Sale sale, List<Product> updatedProducts);
    List<Sale> findAllSales();
}
