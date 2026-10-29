package org.example.model;

import java.util.Locale;

public enum AttendanceStatus {
    PRESENT("Present"),
    LATE("Late"),
    ABSENT("Absent");

    private final String label;

    AttendanceStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean countsAsAttended() {
        return this != ABSENT;
    }

    public static AttendanceStatus fromInput(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "P", "PRESENT" -> PRESENT;
            case "A", "ABSENT" -> ABSENT;
            case "L", "LATE" -> LATE;
            default -> null;
        };
    }
}
