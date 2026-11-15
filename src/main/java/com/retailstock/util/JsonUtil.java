package com.retailstock.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.retailstock.exception.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class JsonUtil {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private JsonUtil() { }

    public static <T> T fromJson(InputStream input, Class<T> type) throws IOException {
        String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) throw new ApiException(400, "Request body must contain valid JSON.");
        try { return GSON.fromJson(body, type); }
        catch (JsonParseException | IllegalStateException ex) { throw new ApiException(400, "Invalid JSON request body."); }
    }
    public static String toJson(Object value) { return GSON.toJson(value); }
}
