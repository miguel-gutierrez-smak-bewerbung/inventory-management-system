package de.resume.inventory.management.system.productservice.models.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {

    ELECTRONICS("electronics"),
    FOOD("food"),
    DRINKS("drinks"),
    HOUSEHOLD("household"),
    SPORTS("sports"),
    FASHION("fashion"),
    HEALTH("health"),
    TOYS("toys"),
    OFFICE("office"),
    AUTOMOTIVE("automotive"),
    OTHER("other");

    private final String name;
}
