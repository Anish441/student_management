package org.example.storage;

import org.example.model.AttendanceRecord;
import org.example.model.AttendanceStatus;
import org.example.model.Course;
import org.example.model.Enrollment;
import org.example.model.Student;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

public final class DataStore {
    private static final List<String> STUDENT_HEADER = List.of("id", "name", "email");
    private static final List<String> COURSE_HEADER = List.of("code", "name");
    private static final List<String> ENROLLMENT_HEADER = List.of("courseCode", "studentId");
    private static final List<String> ATTENDANCE_HEADER = List.of("courseCode", "studentId", "date", "status");

    private final Path directory;

    public DataStore(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath();
    }

    public Path directory() {
        return directory;
    }

    public DataSnapshot load() {
        try {
            List<Student> students = readRows("students.csv", 3).stream()
                    .map(row -> parse(row, () -> new Student(
                            row.cells().get(0), row.cells().get(1), row.cells().get(2))))
                    .toList();
            List<Course> courses = readRows("courses.csv", 2).stream()
                    .map(row -> parse(row, () -> new Course(row.cells().get(0), row.cells().get(1))))
                    .toList();
            List<Enrollment> enrollments = readRows("enrollments.csv", 2).stream()
                    .map(row -> parse(row, () -> new Enrollment(row.cells().get(0), row.cells().get(1))))
                    .toList();
            List<AttendanceRecord> attendance = readRows("attendance.csv", 4).stream()
                    .map(row -> parse(row, () -> new AttendanceRecord(
                            row.cells().get(0),
                            row.cells().get(1),
                            LocalDate.parse(row.cells().get(2)),
                            AttendanceStatus.valueOf(row.cells().get(3).trim().toUpperCase(Locale.ROOT)))))
                    .toList();
            return new DataSnapshot(students, courses, enrollments, attendance);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read data files from " + directory, e);
        }
    }

    public void save(DataSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        try {
            Files.createDirectories(directory);
            write("students.csv", STUDENT_HEADER, snapshot.students().stream()
                    .map(student -> List.of(student.id(), student.name(), student.email()))
                    .toList());
            write("courses.csv", COURSE_HEADER, snapshot.courses().stream()
                    .map(course -> List.of(course.code(), course.name()))
                    .toList());
            write("enrollments.csv", ENROLLMENT_HEADER, snapshot.enrollments().stream()
                    .map(enrollment -> List.of(enrollment.courseCode(), enrollment.studentId()))
                    .toList());
            write("attendance.csv", ATTENDANCE_HEADER, snapshot.attendance().stream()
                    .map(record -> List.of(
                            record.courseCode(),
                            record.studentId(),
                            record.date().toString(),
                            record.status().name()))
                    .toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save data files to " + directory, e);
        }
    }

    private List<Row> readRows(String fileName, int expectedColumns) throws IOException {
        Path file = directory.resolve(fileName);
        if (!Files.exists(file)) {
            return List.of();
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<Row> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            List<String> cells;
            try {
                cells = CsvCodec.decode(line);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(fileName + " line " + (index + 1) + ": " + e.getMessage(), e);
            }
            if (cells.size() != expectedColumns) {
                throw new IllegalArgumentException(fileName + " line " + (index + 1) + ": expected "
                        + expectedColumns + " columns but found " + cells.size() + ".");
            }
            rows.add(new Row(fileName, index + 1, List.copyOf(cells)));
        }
        return rows;
    }

    private static <T> T parse(Row row, Supplier<T> parser) {
        try {
            return parser.get();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(row.file() + " line " + row.line() + ": " + e.getMessage(), e);
        }
    }

    private void write(String fileName, List<String> header, List<List<String>> rows) throws IOException {
        Path target = directory.resolve(fileName);
        Path temp = directory.resolve(fileName + ".tmp");
        StringBuilder content = new StringBuilder(CsvCodec.encode(header)).append('\n');
        for (List<String> row : rows) {
            content.append(CsvCodec.encode(row)).append('\n');
        }
        Files.writeString(temp, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record Row(String file, int line, List<String> cells) {
    }
}
