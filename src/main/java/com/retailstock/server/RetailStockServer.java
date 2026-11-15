package com.retailstock.server;

import com.retailstock.config.AppConfig;
import com.retailstock.controller.ApiController;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class RetailStockServer {
    private final HttpServer server;
    public RetailStockServer(AppConfig config, int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        ApiController controller = new ApiController(config.inventoryService());
        server.createContext("/api/", new ApiHandler(controller));
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors())));
    }
    public void start() { server.start(); }
    public void stop() { server.stop(1); }
}
