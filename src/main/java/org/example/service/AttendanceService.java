package org.example.service;

import org.example.model.AttendanceRecord;
import org.example.model.AttendanceStatus;
import org.example.model.AttendanceSummary;
import org.example.model.Course;
import org.example.model.Enrollment;
import org.example.model.Student;
import org.example.storage.DataSnapshot;
import org.example.storage.DataStore;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class AttendanceService {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^@\\s]+@[^@\\s]+");

    private final DataStore store;
    private final Map<String, Student> students = new TreeMap<>();
    private final Map<String, Course> courses = new TreeMap<>();
    private final Map<String, Set<String>> enrollments = new TreeMap<>();
    private final Map<String, AttendanceRecord> attendance = new TreeMap<>();

    public AttendanceService(DataStore store, DataSnapshot snapshot) {
        this.store = Objects.requireNonNull(store, "store");
        Objects.requireNonNull(snapshot, "snapshot");

        for (Student student : snapshot.students()) {
            if (students.putIfAbsent(student.id(), student) != null) {
                throw new IllegalArgumentException("Duplicate student id in data: " + student.id());
            }
        }
        for (Course course : snapshot.courses()) {
            if (courses.putIfAbsent(course.code(), course) != null) {
                throw new IllegalArgumentException("Duplicate course code in data: " + course.code());
            }
        }
        for (Enrollment enrollment : snapshot.enrollments()) {
            requireStudentExists(enrollment.studentId(), "enrollment");
            requireCourseExists(enrollment.courseCode(), "enrollment");
            enrollments.computeIfAbsent(enrollment.courseCode(), key -> new TreeSet<>())
                    .add(enrollment.studentId());
        }
        for (AttendanceRecord record : snapshot.attendance()) {
            requireStudentExists(record.studentId(), "attendance record");
            requireCourseExists(record.courseCode(), "attendance record");
            attendance.put(key(record.courseCode(), record.studentId(), record.date()), record);
        }
    }

    public Path dataDirectory() {
        return store.directory();
    }

    public List<Student> listStudents() {
        return List.copyOf(students.values());
    }

    public List<Course> listCourses() {
        return List.copyOf(courses.values());
    }

    public Optional<Student> findStudent(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(students.get(normalizeId(id, "Student id")));
    }

    public Optional<Course> findCourse(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(courses.get(normalizeId(code, "Course code")));
    }

    public Student addStudent(String id, String name, String email) {
        String normalizedId = normalizeId(id, "Student id");
        if (students.containsKey(normalizedId)) {
            throw new IllegalArgumentException("A student with ID " + normalizedId + " already exists.");
        }
        Student student = new Student(normalizedId, normalizeName(name, "Student name"), normalizeEmail(email));
        students.put(normalizedId, student);
        persist();
        return student;
    }

    public Student updateStudent(String id, String name, String email) {
        String normalizedId = normalizeId(id, "Student id");
        if (!students.containsKey(normalizedId)) {
            throw new IllegalArgumentException("No student found with ID " + normalizedId + ".");
        }
        Student updated = new Student(normalizedId, normalizeName(name, "Student name"), normalizeEmail(email));
        students.put(normalizedId, updated);
        persist();
        return updated;
    }

    public int deleteStudent(String id) {
        String normalizedId = normalizeId(id, "Student id");
        if (students.remove(normalizedId) == null) {
            throw new IllegalArgumentException("No student found with ID " + normalizedId + ".");
        }
        for (Set<String> roster : enrollments.values()) {
            roster.remove(normalizedId);
        }
        enrollments.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        int removedRecords = 0;
        Iterator<AttendanceRecord> iterator = attendance.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().studentId().equals(normalizedId)) {
                iterator.remove();
                removedRecords++;
            }
        }
        persist();
        return removedRecords;
    }

    public Course addCourse(String code, String name) {
        String normalizedCode = normalizeId(code, "Course code");
        if (courses.containsKey(normalizedCode)) {
            throw new IllegalArgumentException("A course with code " + normalizedCode + " already exists.");
        }
        Course course = new Course(normalizedCode, normalizeName(name, "Course name"));
        courses.put(normalizedCode, course);
        persist();
        return course;
    }

    public Course updateCourse(String code, String name) {
        String normalizedCode = normalizeId(code, "Course code");
        if (!courses.containsKey(normalizedCode)) {
            throw new IllegalArgumentException("No course found with code " + normalizedCode + ".");
        }
        Course updated = new Course(normalizedCode, normalizeName(name, "Course name"));
        courses.put(normalizedCode, updated);
        persist();
        return updated;
    }

    public int deleteCourse(String code) {
        String normalizedCode = normalizeId(code, "Course code");
        if (courses.remove(normalizedCode) == null) {
            throw new IllegalArgumentException("No course found with code " + normalizedCode + ".");
        }
        enrollments.remove(normalizedCode);
        int removedRecords = 0;
        Iterator<AttendanceRecord> iterator = attendance.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().courseCode().equals(normalizedCode)) {
                iterator.remove();
                removedRecords++;
            }
        }
        persist();
        return removedRecords;
    }

    public void enroll(String courseCode, String studentId) {
        String code = normalizeId(courseCode, "Course code");
        String id = normalizeId(studentId, "Student id");
        Course course = requireCourse(code);
        Student student = requireStudent(id);
        Set<String> roster = enrollments.computeIfAbsent(code, key -> new TreeSet<>());
        if (!roster.add(id)) {
            throw new IllegalArgumentException(student.name() + " (" + id + ") is already enrolled in "
                    + course.code() + ".");
        }
        persist();
    }

    public void unenroll(String courseCode, String studentId) {
        String code = normalizeId(courseCode, "Course code");
        String id = normalizeId(studentId, "Student id");
        requireCourse(code);
        requireStudent(id);
        Set<String> roster = enrollments.get(code);
        if (roster == null || !roster.remove(id)) {
            throw new IllegalArgumentException("Student " + id + " is not enrolled in " + code + ".");
        }
        if (roster.isEmpty()) {
            enrollments.remove(code);
        }
        persist();
    }

    public boolean isEnrolled(String courseCode, String studentId) {
        String code = normalizeId(courseCode, "Course code");
        String id = normalizeId(studentId, "Student id");
        return enrollments.getOrDefault(code, Collections.emptySet()).contains(id);
    }

    public List<Student> enrolledStudents(String courseCode) {
        String code = normalizeId(courseCode, "Course code");
        requireCourse(code);
        return enrollments.getOrDefault(code, Collections.emptySet()).stream()
                .map(students::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<Course> enrolledCourses(String studentId) {
        String id = normalizeId(studentId, "Student id");
        requireStudent(id);
        List<Course> result = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : enrollments.entrySet()) {
            if (entry.getValue().contains(id)) {
                result.add(courses.get(entry.getKey()));
            }
        }
        return List.copyOf(result);
    }

    public Optional<AttendanceRecord> findAttendance(String courseCode, String studentId, LocalDate date) {
        String code = normalizeId(courseCode, "Course code");
        String id = normalizeId(studentId, "Student id");
        Objects.requireNonNull(date, "Date is required.");
        return Optional.ofNullable(attendance.get(key(code, id, date)));
    }

    public int markAttendance(String courseCode, LocalDate date, Map<String, AttendanceStatus> statuses) {
        String code = normalizeId(courseCode, "Course code");
        Course course = requireCourse(code);
        validateAttendanceDate(date);
        Objects.requireNonNull(statuses, "statuses");
        Set<String> roster = enrollments.getOrDefault(code, Collections.emptySet());
        Map<String, AttendanceRecord> updates = new LinkedHashMap<>();
        for (Map.Entry<String, AttendanceStatus> entry : statuses.entrySet()) {
            String id = normalizeId(entry.getKey(), "Student id");
            Student student = requireStudent(id);
            if (!roster.contains(id)) {
                throw new IllegalArgumentException(student.name() + " (" + id + ") is not enrolled in "
                        + course.code() + ".");
            }
            AttendanceStatus status = Objects.requireNonNull(entry.getValue(), "Attendance status is required.");
            updates.put(key(code, id, date), new AttendanceRecord(code, id, date, status));
        }
        if (updates.isEmpty()) {
            return 0;
        }
        attendance.putAll(updates);
        persist();
        return updates.size();
    }

    public List<AttendanceRecord> recordsForCourse(String courseCode) {
        String code = normalizeId(courseCode, "Course code");
        requireCourse(code);
        return attendance.values().stream()
                .filter(record -> record.courseCode().equals(code))
                .sorted(Comparator.comparing(AttendanceRecord::date).thenComparing(AttendanceRecord::studentId))
                .toList();
    }

    public List<AttendanceRecord> recordsForCourseDate(String courseCode, LocalDate date) {
        String code = normalizeId(courseCode, "Course code");
        requireCourse(code);
        Objects.requireNonNull(date, "Date is required.");
        return attendance.values().stream()
                .filter(record -> record.courseCode().equals(code) && record.date().equals(date))
                .sorted(Comparator.comparing(AttendanceRecord::studentId))
                .toList();
    }

    public List<AttendanceRecord> recordsForStudent(String studentId) {
        String id = normalizeId(studentId, "Student id");
        requireStudent(id);
        return attendance.values().stream()
                .filter(record -> record.studentId().equals(id))
                .sorted(Comparator.comparing(AttendanceRecord::date).thenComparing(AttendanceRecord::courseCode))
                .toList();
    }

    public AttendanceSummary summaryForStudent(String studentId) {
        return AttendanceSummary.of(recordsForStudent(studentId));
    }

    public AttendanceSummary summaryForStudentCourse(String studentId, String courseCode) {
        String id = normalizeId(studentId, "Student id");
        String code = normalizeId(courseCode, "Course code");
        requireStudent(id);
        requireCourse(code);
        return AttendanceSummary.of(attendance.values().stream()
                .filter(record -> record.studentId().equals(id) && record.courseCode().equals(code))
                .toList());
    }

    public AttendanceSummary summaryForCourse(String courseCode) {
        return AttendanceSummary.of(recordsForCourse(courseCode));
    }

    public AttendanceSummary summaryForCourseDate(String courseCode, LocalDate date) {
        return AttendanceSummary.of(recordsForCourseDate(courseCode, date));
    }

    public AttendanceSummary overallSummary() {
        return AttendanceSummary.of(attendance.values());
    }

    public Map<Course, AttendanceSummary> courseSummariesForStudent(String studentId) {
        String id = normalizeId(studentId, "Student id");
        requireStudent(id);
        Map<String, List<AttendanceRecord>> grouped = new TreeMap<>();
        for (AttendanceRecord record : attendance.values()) {
            if (record.studentId().equals(id)) {
                grouped.computeIfAbsent(record.courseCode(), key -> new ArrayList<>()).add(record);
            }
        }
        Map<Course, AttendanceSummary> summaries = new LinkedHashMap<>();
        grouped.forEach((code, records) -> summaries.put(courses.get(code), AttendanceSummary.of(records)));
        return summaries;
    }

    public int studentCount() {
        return students.size();
    }

    public int courseCount() {
        return courses.size();
    }

    public int enrollmentCount() {
        return enrollments.values().stream().mapToInt(Set::size).sum();
    }

    public int attendanceCount() {
        return attendance.size();
    }

    private void persist() {
        List<Enrollment> enrollmentRows = new ArrayList<>();
        enrollments.forEach((code, roster) ->
                roster.forEach(id -> enrollmentRows.add(new Enrollment(code, id))));
        store.save(new DataSnapshot(
                List.copyOf(students.values()),
                List.copyOf(courses.values()),
                enrollmentRows,
                List.copyOf(attendance.values())));
    }

    private Student requireStudent(String id) {
        Student student = students.get(id);
        if (student == null) {
            throw new IllegalArgumentException("No student found with ID " + id + ".");
        }
        return student;
    }

    private Course requireCourse(String code) {
        Course course = courses.get(code);
        if (course == null) {
            throw new IllegalArgumentException("No course found with code " + code + ".");
        }
        return course;
    }

    private void requireStudentExists(String id, String source) {
        if (!students.containsKey(id)) {
            throw new IllegalArgumentException("Data error: " + source + " references unknown student " + id + ".");
        }
    }

    private void requireCourseExists(String code, String source) {
        if (!courses.containsKey(code)) {
            throw new IllegalArgumentException("Data error: " + source + " references unknown course " + code + ".");
        }
    }

    private static void validateAttendanceDate(LocalDate date) {
        Objects.requireNonNull(date, "Date is required.");
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Attendance cannot be recorded for a future date.");
        }
    }

    private static String normalizeId(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 20) {
            throw new IllegalArgumentException(label + " must be 20 characters or fewer.");
        }
        if (!ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(label
                    + " may only contain letters, numbers, dots, underscores or hyphens.");
        }
        return normalized;
    }

    private static String normalizeName(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        String name = value.trim();
        if (name.length() > 80) {
            throw new IllegalArgumentException(label + " must be 80 characters or fewer.");
        }
        return name;
    }

    private static String normalizeEmail(String value) {
        if (value == null) {
            return "";
        }
        String email = value.trim();
        if (email.isEmpty()) {
            return "";
        }
        if (email.length() > 120) {
            throw new IllegalArgumentException("Email must be 120 characters or fewer.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email address is not valid.");
        }
        return email;
    }

    private static String key(String courseCode, String studentId, LocalDate date) {
        return courseCode + '|' + studentId + '|' + date;
    }
}
