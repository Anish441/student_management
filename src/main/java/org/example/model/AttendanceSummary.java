package org.example.model;

import java.util.Collection;

public record AttendanceSummary(int present, int late, int absent) {
    public AttendanceSummary {
        if (present < 0 || late < 0 || absent < 0) {
            throw new IllegalArgumentException("Attendance counts cannot be negative.");
        }
    }

    public static AttendanceSummary of(Collection<AttendanceRecord> records) {
        int present = 0;
        int late = 0;
        int absent = 0;
        for (AttendanceRecord record : records) {
            switch (record.status()) {
                case PRESENT -> present++;
                case LATE -> late++;
                case ABSENT -> absent++;
            }
        }
        return new AttendanceSummary(present, late, absent);
    }

    public int total() {
        return present + late + absent;
    }

    public int attended() {
        return present + late;
    }

    public double percentage() {
        return total() == 0 ? 0.0 : attended() * 100.0 / total();
    }
}
