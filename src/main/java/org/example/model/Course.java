package org.example.model;

import java.util.Locale;

public record Course(String code, String name) {
    public Course {
        code = required(code, "Course code").toUpperCase(Locale.ROOT);
        name = required(name, "Course name");
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
