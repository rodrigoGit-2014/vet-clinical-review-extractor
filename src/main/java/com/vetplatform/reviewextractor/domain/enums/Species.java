package com.vetplatform.reviewextractor.domain.enums;

import java.util.Arrays;

public enum Species {
    DOG, CAT, BIRD, RABBIT, HAMSTER, FISH, REPTILE, HORSE, TURTLE, GUINEA_PIG, FERRET, OTHER;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(s -> s.name().equalsIgnoreCase(value));
    }
}
