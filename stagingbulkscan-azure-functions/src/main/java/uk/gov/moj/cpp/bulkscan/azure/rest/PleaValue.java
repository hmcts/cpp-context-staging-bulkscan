package uk.gov.moj.cpp.bulkscan.azure.rest;

import java.util.Optional;

public enum PleaValue {
    NOT_GUILTY("NOT_GUILTY"),

    GUILTY("GUILTY"),

    BOTH("BOTH");

    private final String value;

    PleaValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static Optional<PleaValue> valueFor(final String value) {
        if (NOT_GUILTY.value.equals(value)) {
            return Optional.of(NOT_GUILTY);
        }

        if (GUILTY.value.equals(value)) {
            return Optional.of(GUILTY);
        }

        if (BOTH.value.equals(value)) {
            return Optional.of(BOTH);
        }

        return Optional.empty();
    }
}
