package de.resume.inventory.management.system.productservice.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductAction {
    CREATED("created"),
    UPDATED("updated"),
    DELETED("deleted");

    private final String action;
}
