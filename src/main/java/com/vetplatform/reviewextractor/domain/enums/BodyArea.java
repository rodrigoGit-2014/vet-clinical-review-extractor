package com.vetplatform.reviewextractor.domain.enums;

import java.util.Arrays;

public enum BodyArea {
    HEAD, NECK, THORAX, ABDOMEN, LIMBS, ANAL, SKIN, ORAL, OCULAR, AURICULAR, GENERAL;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(b -> b.name().equalsIgnoreCase(value));
    }
}
