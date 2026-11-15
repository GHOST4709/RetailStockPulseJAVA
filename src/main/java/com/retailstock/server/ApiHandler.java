package com.retailstock.server;

import com.retailstock.controller.ApiController;
import com.retailstock.dto.ProductRequest;
import com.retailstock.dto.SaleRequest;
import com.retailstock.dto.TransactionRequest;
import com.retailstock.exception.ApiException;
import com.retailstock.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiHandler implements HttpHandler {
    private final ApiController controller;
    public ApiHandler(ApiController controller) { this.controller = controller; }

    @Override public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();
            if ("OPTIONS".equals(method)) { send(exchange, 204, ""); return; }
            Object result;
            int status = 200;

            if ("GET".equals(method) && "/api/health".equals(path)) {
                result = Map.of("status", "UP", "application", "Retail Stock Pulse");
            } else if ("GET".equals(method) && "/api/dashboard".equals(path)) {
                result = controller.service().dashboard();
            } else if ("GET".equals(method) && "/api/products".equals(path)) {
                result = controller.service().getProducts(queryParam(exchange, "search"));
            } else if ("POST".equals(method) && "/api/products".equals(path)) {
                result = controller.createProduct(JsonUtil.fromJson(exchange.getRequestBody(), ProductRequest.class)); status = 201;
            } else if ("GET".equals(method) && "/api/low-stock".equals(path)) {
                result = controller.service().getLowStockProducts();
            } else if ("GET".equals(method) && "/api/purchases".equals(path)) {
                result = controller.service().getPurchases();
            } else if ("POST".equals(method) && "/api/purchases".equals(path)) {
                result = controller.recordPurchase(JsonUtil.fromJson(exchange.getRequestBody(), TransactionRequest.class)); status = 201;
            } else if ("GET".equals(method) && "/api/sales".equals(path)) {
                result = controller.service().getSales();
            } else if ("POST".equals(method) && "/api/sales".equals(path)) {
                result = controller.recordSale(JsonUtil.fromJson(exchange.getRequestBody(), SaleRequest.class)); status = 201;
            } else if ("GET".equals(method) && "/api/reports/valuation".equals(path)) {
                result = controller.service().valuationReport();
            } else if ("GET".equals(method) && "/api/reports/fast-moving".equals(path)) {
                result = controller.service().fastMovingReport();
            } else if ("GET".equals(method) && "/api/reports/sales".equals(path)) {
                result = controller.service().salesReport(queryParam(exchange, "from"), queryParam(exchange, "to"));
            } else if (path.matches("/api/products/[0-9]+")) {
                long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
                if ("GET".equals(method)) result = controller.service().getProduct(id);
                else if ("PUT".equals(method)) result = controller.updateProduct(id, JsonUtil.fromJson(exchange.getRequestBody(), ProductRequest.class));
                else if ("DELETE".equals(method)) { controller.service().deactivateProduct(id); result = Map.of("message", "Product deactivated."); }
                else throw new ApiException(405, "Method not allowed.");
            } else {
                throw new ApiException(404, "API endpoint not found.");
            }
            send(exchange, status, JsonUtil.toJson(result));
        } catch (ApiException ex) {
            send(exchange, ex.getStatusCode(), JsonUtil.toJson(Map.of("error", ex.getMessage())));
        } catch (Exception ex) {
            ex.printStackTrace();
            send(exchange, 500, JsonUtil.toJson(Map.of("error", "An unexpected server error occurred.")));
        } finally {
            exchange.close();
        }
    }

    private static String queryParam(HttpExchange exchange, String key) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && URLDecoder.decode(parts[0], StandardCharsets.UTF_8).equals(key))
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
        }
        return null;
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        if (status == 204) { exchange.sendResponseHeaders(status, -1); return; }
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
