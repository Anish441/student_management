package org.example.model;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

public record AttendanceRecord(String courseCode, String studentId, LocalDate date, AttendanceStatus status) {
    public AttendanceRecord {
        courseCode = required(courseCode, "Course code").toUpperCase(Locale.ROOT);
        studentId = required(studentId, "Student id").toUpperCase(Locale.ROOT);
        date = Objects.requireNonNull(date, "Date is required.");
        status = Objects.requireNonNull(status, "Attendance status is required.");
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
