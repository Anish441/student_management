package org.example.storage;

import org.example.model.AttendanceRecord;
import org.example.model.Course;
import org.example.model.Enrollment;
import org.example.model.Student;

import java.util.List;

public record DataSnapshot(
        List<Student> students,
        List<Course> courses,
        List<Enrollment> enrollments,
        List<AttendanceRecord> attendance) {

    public DataSnapshot {
        students = List.copyOf(students);
        courses = List.copyOf(courses);
        enrollments = List.copyOf(enrollments);
        attendance = List.copyOf(attendance);
    }

    public static DataSnapshot empty() {
        return new DataSnapshot(List.of(), List.of(), List.of(), List.of());
    }
}
