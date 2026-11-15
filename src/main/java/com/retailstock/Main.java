package com.retailstock;

import com.retailstock.config.AppConfig;
import com.retailstock.server.RetailStockServer;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        AppConfig config = new AppConfig();
        RetailStockServer server = new RetailStockServer(config, port);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        System.out.println("============================================");
        System.out.println("          RETAIL STOCK PULSE                ");
        System.out.println("============================================");
        System.out.println("Server started at: http://localhost:" + port);
        System.out.println("API health:        http://localhost:" + port + "/api/health");
        System.out.println("Press Ctrl+C to stop the server.");
    }
}
