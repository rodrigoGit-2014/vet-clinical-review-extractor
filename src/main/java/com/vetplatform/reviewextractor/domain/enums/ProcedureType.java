package com.vetplatform.reviewextractor.domain.enums;

import java.util.Arrays;

public enum ProcedureType {
    SURGICAL, DIAGNOSTIC, THERAPEUTIC, PREVENTIVE;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(p -> p.name().equalsIgnoreCase(value));
    }
}
