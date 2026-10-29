package org.example.model;

import java.util.Locale;

public record Enrollment(String courseCode, String studentId) {
    public Enrollment {
        courseCode = required(courseCode, "Course code").toUpperCase(Locale.ROOT);
        studentId = required(studentId, "Student id").toUpperCase(Locale.ROOT);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
