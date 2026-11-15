package com.retailstock.exception;

public class InsufficientStockException extends ApiException {
    public InsufficientStockException(String message) { super(409, message); }
}
