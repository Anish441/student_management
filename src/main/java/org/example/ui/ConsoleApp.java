package org.example.ui;

import org.example.model.AttendanceRecord;
import org.example.model.AttendanceStatus;
import org.example.model.AttendanceSummary;
import org.example.model.Course;
import org.example.model.Student;
import org.example.service.AttendanceService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public final class ConsoleApp {
    private final AttendanceService service;
    private final InputReader input;

    public ConsoleApp(AttendanceService service) {
        this.service = service;
        this.input = new InputReader(new Scanner(System.in));
    }

    public void run() {
        System.out.println("==============================");
        System.out.println("   Student Attendance System");
        System.out.println("==============================");
        System.out.println("Data directory: " + service.dataDirectory());
        try {
            boolean running = true;
            while (running) {
                printMainMenu();
                int choice = input.readInt("Choose an option: ", 0, 5);
                try {
                    switch (choice) {
                        case 1 -> studentMenu();
                        case 2 -> courseMenu();
                        case 3 -> takeAttendance();
                        case 4 -> attendanceMenu();
                        case 5 -> reportsMenu();
                        default -> running = false;
                    }
                } catch (InputReader.InputClosedException e) {
                    throw e;
                } catch (RuntimeException e) {
                    System.out.println("  Error: " + e.getMessage());
                }
            }
        } catch (InputReader.InputClosedException e) {
            System.out.println();
        }
        System.out.println("Goodbye.");
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("Main menu");
        System.out.println("  1. Students");
        System.out.println("  2. Courses & enrollment");
        System.out.println("  3. Take attendance");
        System.out.println("  4. View attendance");
        System.out.println("  5. Reports");
        System.out.println("  0. Exit");
    }

    private void studentMenu() {
        while (true) {
            System.out.println();
            System.out.println("Students");
            System.out.println("  1. List students");
            System.out.println("  2. Add student");
            System.out.println("  3. Edit student");
            System.out.println("  4. Delete student");
            System.out.println("  0. Back");
            int choice = input.readInt("Choose an option: ", 0, 4);
            switch (choice) {
                case 1 -> listStudents();
                case 2 -> addStudent();
                case 3 -> editStudent();
                case 4 -> deleteStudent();
                default -> {
                    return;
                }
            }
        }
    }

    private void listStudents() {
        List<Student> students = service.listStudents();
        if (students.isEmpty()) {
            System.out.println("  No students yet.");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (Student student : students) {
            rows.add(List.of(
                    student.id(),
                    student.name(),
                    student.email().isBlank() ? "-" : student.email(),
                    Integer.toString(service.enrolledCourses(student.id()).size())));
        }
        Table.print(List.of("ID", "Name", "Email", "Courses"), rows);
    }

    private void addStudent() {
        String id = input.readRequired("  Student ID: ");
        String name = input.readRequired("  Full name: ");
        String email = input.readLine("  Email (optional): ");
        Student student = service.addStudent(id, name, email);
        System.out.println("  Added student " + student.name() + " (" + student.id() + ").");
    }

    private void editStudent() {
        Student existing = chooseStudent();
        String name = input.readLine("  Full name [" + existing.name() + "]: ");
        if (name.isEmpty()) {
            name = existing.name();
        }
        String email = input.readLine("  Email ["
                + (existing.email().isBlank() ? "-" : existing.email()) + "]: ");
        if (email.isEmpty()) {
            email = existing.email();
        }
        Student updated = service.updateStudent(existing.id(), name, email);
        System.out.println("  Updated " + updated.name() + " (" + updated.id() + ").");
    }

    private void deleteStudent() {
        Student student = chooseStudent();
        if (!input.confirm("  Delete " + student.name() + " (" + student.id()
                + ") and their attendance records?")) {
            System.out.println("  Cancelled.");
            return;
        }
        int removedRecords = service.deleteStudent(student.id());
        System.out.println("  Deleted student " + student.id() + " and "
                + removedRecords + " attendance record(s).");
    }

    private void courseMenu() {
        while (true) {
            System.out.println();
            System.out.println("Courses & enrollment");
            System.out.println("  1. List courses");
            System.out.println("  2. Add course");
            System.out.println("  3. Rename course");
            System.out.println("  4. Enroll student");
            System.out.println("  5. Unenroll student");
            System.out.println("  6. View course roster");
            System.out.println("  7. Delete course");
            System.out.println("  0. Back");
            int choice = input.readInt("Choose an option: ", 0, 7);
            switch (choice) {
                case 1 -> listCourses();
                case 2 -> addCourse();
                case 3 -> renameCourse();
                case 4 -> enrollStudent();
                case 5 -> unenrollStudent();
                case 6 -> viewRoster();
                case 7 -> deleteCourse();
                default -> {
                    return;
                }
            }
        }
    }

    private void listCourses() {
        List<Course> courses = service.listCourses();
        if (courses.isEmpty()) {
            System.out.println("  No courses yet.");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (Course course : courses) {
            rows.add(List.of(
                    course.code(),
                    course.name(),
                    Integer.toString(service.enrolledStudents(course.code()).size())));
        }
        Table.print(List.of("Code", "Course", "Enrolled"), rows);
    }

    private void addCourse() {
        String code = input.readRequired("  Course code: ");
        String name = input.readRequired("  Course name: ");
        Course course = service.addCourse(code, name);
        System.out.println("  Added course " + course.code() + " - " + course.name() + ".");
    }

    private void renameCourse() {
        Course course = chooseCourse();
        String name = input.readLine("  Course name [" + course.name() + "]: ");
        if (name.isEmpty()) {
            name = course.name();
        }
        Course updated = service.updateCourse(course.code(), name);
        System.out.println("  Renamed " + updated.code() + " to " + updated.name() + ".");
    }

    private void enrollStudent() {
        Course course = chooseCourse();
        Student student = chooseStudent();
        service.enroll(course.code(), student.id());
        System.out.println("  Enrolled " + student.name() + " (" + student.id()
                + ") in " + course.code() + ".");
    }

    private void unenrollStudent() {
        Course course = chooseCourse();
        Student student = chooseStudent();
        if (!service.isEnrolled(course.code(), student.id())) {
            throw new IllegalArgumentException(student.name() + " (" + student.id()
                    + ") is not enrolled in " + course.code() + ".");
        }
        if (!input.confirm("  Remove " + student.name() + " from " + course.code() + "?")) {
            System.out.println("  Cancelled.");
            return;
        }
        service.unenroll(course.code(), student.id());
        System.out.println("  Unenrolled " + student.name() + " from " + course.code() + ".");
    }

    private void viewRoster() {
        Course course = chooseCourse();
        List<Student> roster = service.enrolledStudents(course.code());
        if (roster.isEmpty()) {
            System.out.println("  No students enrolled in " + course.code() + ".");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (Student student : roster) {
            AttendanceSummary summary = service.summaryForStudentCourse(student.id(), course.code());
            rows.add(List.of(
                    student.id(),
                    student.name(),
                    Integer.toString(summary.attended()),
                    Integer.toString(summary.total()),
                    percent(summary)));
        }
        System.out.println("  Roster for " + course.code() + " - " + course.name());
        Table.print(List.of("ID", "Name", "Attended", "Records", "Attendance %"), rows);
    }

    private void deleteCourse() {
        Course course = chooseCourse();
        if (!input.confirm("  Delete " + course.code() + " - " + course.name()
                + " with its enrollment and attendance records?")) {
            System.out.println("  Cancelled.");
            return;
        }
        int removedRecords = service.deleteCourse(course.code());
        System.out.println("  Deleted course " + course.code() + " and "
                + removedRecords + " attendance record(s).");
    }

    private void takeAttendance() {
        Course course = chooseCourse();
        List<Student> roster = service.enrolledStudents(course.code());
        if (roster.isEmpty()) {
            System.out.println("  No students enrolled in " + course.code()
                    + ". Enroll students from the Courses menu first.");
            return;
        }
        LocalDate date = input.readDate("  Date (YYYY-MM-DD) [today]: ", LocalDate.now());
        System.out.println("  Marking attendance for " + course.code() + " on " + date + ".");
        System.out.println("  Enter P=present, A=absent, L=late, S=skip, or press Enter to keep the current value.");

        Map<String, AttendanceStatus> changes = new LinkedHashMap<>();
        for (Student student : roster) {
            AttendanceRecord existing = service.findAttendance(course.code(), student.id(), date).orElse(null);
            String current = existing == null ? "not marked" : existing.status().label();
            while (true) {
                String answer = input.readLine(String.format("  %-8s %-25s [%s] P/A/L/S: ",
                        student.id(), student.name(), current));
                if (answer.isEmpty() || answer.equalsIgnoreCase("s") || answer.equalsIgnoreCase("skip")) {
                    break;
                }
                AttendanceStatus status = AttendanceStatus.fromInput(answer);
                if (status == null) {
                    System.out.println("    Enter P, A, L or S.");
                    continue;
                }
                changes.put(student.id(), status);
                break;
            }
        }

        if (changes.isEmpty()) {
            System.out.println("  No changes made.");
            return;
        }
        int saved = service.markAttendance(course.code(), date, changes);
        AttendanceSummary summary = service.summaryForCourseDate(course.code(), date);
        System.out.println("  Saved " + saved + " record(s). " + summaryText(summary));
    }

    private void attendanceMenu() {
        while (true) {
            System.out.println();
            System.out.println("View attendance");
            System.out.println("  1. By course and date");
            System.out.println("  2. By course (all records)");
            System.out.println("  3. By student");
            System.out.println("  0. Back");
            int choice = input.readInt("Choose an option: ", 0, 3);
            switch (choice) {
                case 1 -> viewAttendanceByCourseDate();
                case 2 -> viewAttendanceByCourse();
                case 3 -> viewAttendanceByStudent();
                default -> {
                    return;
                }
            }
        }
    }

    private void viewAttendanceByCourseDate() {
        Course course = chooseCourse();
        LocalDate date = input.readDate("  Date (YYYY-MM-DD) [today]: ", LocalDate.now());
        List<AttendanceRecord> records = service.recordsForCourseDate(course.code(), date);
        if (records.isEmpty()) {
            System.out.println("  No attendance recorded for " + course.code() + " on " + date + ".");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (AttendanceRecord record : records) {
            rows.add(List.of(
                    record.studentId(),
                    studentName(record.studentId()),
                    record.status().label()));
        }
        System.out.println("  Attendance for " + course.code() + " - " + course.name() + " on " + date);
        Table.print(List.of("ID", "Name", "Status"), rows);
        System.out.println("  " + summaryText(service.summaryForCourseDate(course.code(), date)));
    }

    private void viewAttendanceByCourse() {
        Course course = chooseCourse();
        List<AttendanceRecord> records = service.recordsForCourse(course.code());
        if (records.isEmpty()) {
            System.out.println("  No attendance recorded for " + course.code() + ".");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (AttendanceRecord record : records) {
            rows.add(List.of(
                    record.date().toString(),
                    record.studentId(),
                    studentName(record.studentId()),
                    record.status().label()));
        }
        System.out.println("  All attendance for " + course.code() + " - " + course.name());
        Table.print(List.of("Date", "ID", "Name", "Status"), rows);
        System.out.println("  " + summaryText(service.summaryForCourse(course.code())));
    }

    private void viewAttendanceByStudent() {
        Student student = chooseStudent();
        List<AttendanceRecord> records = service.recordsForStudent(student.id());
        if (records.isEmpty()) {
            System.out.println("  No attendance recorded for " + student.name() + ".");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (AttendanceRecord record : records) {
            Course course = service.findCourse(record.courseCode()).orElse(null);
            rows.add(List.of(
                    record.date().toString(),
                    record.courseCode(),
                    course == null ? "-" : course.name(),
                    record.status().label()));
        }
        System.out.println("  Attendance for " + student.name() + " (" + student.id() + ")");
        Table.print(List.of("Date", "Code", "Course", "Status"), rows);
        System.out.println("  " + summaryText(service.summaryForStudent(student.id())));
    }

    private void reportsMenu() {
        while (true) {
            System.out.println();
            System.out.println("Reports");
            System.out.println("  1. Student attendance report");
            System.out.println("  2. Course attendance report");
            System.out.println("  3. Overall statistics");
            System.out.println("  0. Back");
            int choice = input.readInt("Choose an option: ", 0, 3);
            switch (choice) {
                case 1 -> studentReport();
                case 2 -> courseReport();
                case 3 -> overallReport();
                default -> {
                    return;
                }
            }
        }
    }

    private void studentReport() {
        Student student = chooseStudent();
        AttendanceSummary overall = service.summaryForStudent(student.id());
        System.out.println("  Attendance report for " + student.name() + " (" + student.id() + ")");
        System.out.println("  " + summaryText(overall));

        Map<Course, AttendanceSummary> byCourse = service.courseSummariesForStudent(student.id());
        if (byCourse.isEmpty()) {
            System.out.println("  No attendance records yet.");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        byCourse.forEach((course, summary) -> rows.add(List.of(
                course.code(),
                course.name(),
                Integer.toString(summary.present()),
                Integer.toString(summary.late()),
                Integer.toString(summary.absent()),
                summary.attended() + "/" + summary.total(),
                percent(summary))));
        Table.print(List.of("Code", "Course", "Present", "Late", "Absent", "Attended", "Attendance %"), rows);
    }

    private void courseReport() {
        Course course = chooseCourse();
        List<AttendanceRecord> records = service.recordsForCourse(course.code());
        if (records.isEmpty()) {
            System.out.println("  No attendance recorded for " + course.code() + ".");
            return;
        }
        Map<LocalDate, List<AttendanceRecord>> sessions = new TreeMap<>();
        for (AttendanceRecord record : records) {
            sessions.computeIfAbsent(record.date(), key -> new ArrayList<>()).add(record);
        }
        List<List<String>> rows = new ArrayList<>();
        sessions.forEach((date, sessionRecords) -> {
            AttendanceSummary summary = AttendanceSummary.of(sessionRecords);
            rows.add(List.of(
                    date.toString(),
                    Integer.toString(summary.present()),
                    Integer.toString(summary.late()),
                    Integer.toString(summary.absent()),
                    Integer.toString(summary.total()),
                    percent(summary)));
        });
        System.out.println("  Course report for " + course.code() + " - " + course.name());
        Table.print(List.of("Date", "Present", "Late", "Absent", "Total", "Attendance %"), rows);
        System.out.println("  " + summaryText(service.summaryForCourse(course.code())));
    }

    private void overallReport() {
        System.out.println("  Overall statistics");
        System.out.println("  Students: " + service.studentCount());
        System.out.println("  Courses: " + service.courseCount());
        System.out.println("  Enrollments: " + service.enrollmentCount());
        System.out.println("  Attendance records: " + service.attendanceCount());
        System.out.println("  " + summaryText(service.overallSummary()));
    }

    private Course chooseCourse() {
        if (service.listCourses().isEmpty()) {
            throw new IllegalArgumentException("No courses available. Add a course first.");
        }
        listCourses();
        String code = input.readRequired("  Course code: ");
        return service.findCourse(code).orElseThrow(() -> new IllegalArgumentException(
                "No course found with code " + code.trim().toUpperCase(Locale.ROOT) + "."));
    }

    private Student chooseStudent() {
        if (service.listStudents().isEmpty()) {
            throw new IllegalArgumentException("No students available. Add a student first.");
        }
        listStudents();
        String id = input.readRequired("  Student ID: ");
        return service.findStudent(id).orElseThrow(() -> new IllegalArgumentException(
                "No student found with ID " + id.trim().toUpperCase(Locale.ROOT) + "."));
    }

    private String studentName(String studentId) {
        return service.findStudent(studentId).map(Student::name).orElse("-");
    }

    private static String summaryText(AttendanceSummary summary) {
        return "Present " + summary.present()
                + ", Late " + summary.late()
                + ", Absent " + summary.absent()
                + " | attended " + summary.attended() + "/" + summary.total()
                + " (" + percent(summary) + ")";
    }

    private static String percent(AttendanceSummary summary) {
        return String.format(Locale.ROOT, "%.1f%%", summary.percentage());
    }
}
