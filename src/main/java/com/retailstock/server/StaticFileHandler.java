package com.retailstock.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StaticFileHandler implements HttpHandler {
    private static final Map<String, String> TYPES = Map.of(
            ".html", "text/html; charset=utf-8", ".css", "text/css; charset=utf-8",
            ".js", "text/javascript; charset=utf-8", ".json", "application/json; charset=utf-8",
            ".svg", "image/svg+xml", ".png", "image/png", ".ico", "image/x-icon"
    );

    @Override public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); return;
            }
            String requested = URLDecoder.decode(exchange.getRequestURI().getPath(), StandardCharsets.UTF_8);
            if (requested.equals("/")) requested = "/index.html";
            if (requested.contains("..") || requested.contains("\\")) { exchange.sendResponseHeaders(400, -1); return; }
            String resource = "/static" + requested;
            try (InputStream input = getClass().getResourceAsStream(resource)) {
                if (input == null) { exchange.sendResponseHeaders(404, -1); return; }
                byte[] bytes = input.readAllBytes();
                int dot = requested.lastIndexOf('.');
                String extension = dot >= 0 ? requested.substring(dot) : "";
                exchange.getResponseHeaders().set("Content-Type", TYPES.getOrDefault(extension, "application/octet-stream"));
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(200, -1); return; }
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        } finally { exchange.close(); }
    }
}
