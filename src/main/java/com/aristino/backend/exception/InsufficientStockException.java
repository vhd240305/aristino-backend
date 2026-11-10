package com.aristino.backend.exception;

// Nem ra khi mot bien the san pham khong du ton kho tai thoi diem dat hang
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}