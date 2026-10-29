package org.example;

import org.example.service.AttendanceService;
import org.example.storage.DataStore;
import org.example.ui.ConsoleApp;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        DataStore store = new DataStore(Path.of(System.getProperty("attendance.data", "data")));
        AttendanceService service;
        try {
            service = new AttendanceService(store, store.load());
        } catch (RuntimeException e) {
            System.err.println("Could not load attendance data: " + e.getMessage());
            return;
        }
        new ConsoleApp(service).run();
    }
}
