package com.retailstock.exception;

public class DuplicateSkuException extends ApiException {
    public DuplicateSkuException(String message) { super(409, message); }
}
