package org.example.model;

import java.util.Locale;

public record Student(String id, String name, String email) {
    public Student {
        id = required(id, "Student id").toUpperCase(Locale.ROOT);
        name = required(name, "Student name");
        email = email == null ? "" : email.trim();
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
