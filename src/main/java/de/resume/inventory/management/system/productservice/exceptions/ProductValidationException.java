package de.resume.inventory.management.system.productservice.exceptions;

public class ProductValidationException extends RuntimeException {
    public ProductValidationException(final String message) {
        super(message);
    }
}
