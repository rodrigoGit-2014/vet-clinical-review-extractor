package com.vetplatform.reviewextractor.domain.enums;

import java.util.Arrays;

public enum OutcomeStatus {
    FULLY_RECOVERED, IMPROVING, STABLE, WORSENING, DECEASED, UNKNOWN;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(o -> o.name().equalsIgnoreCase(value));
    }
}
