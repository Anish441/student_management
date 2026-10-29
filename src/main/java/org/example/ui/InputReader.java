package org.example.ui;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class InputReader {
    private final Scanner scanner;

    public InputReader(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new InputClosedException();
        }
        return scanner.nextLine().trim();
    }

    public String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("  Value is required.");
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String value = readLine(prompt);
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("  Enter a number between " + min + " and " + max + ".");
        }
    }

    public LocalDate readDate(String prompt, LocalDate defaultValue) {
        while (true) {
            String value = readLine(prompt);
            if (value.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            if (value.equalsIgnoreCase("today")) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("  Use the format YYYY-MM-DD.");
            }
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String value = readLine(prompt + " [y/N]: ");
            if (value.isEmpty() || value.equalsIgnoreCase("n") || value.equalsIgnoreCase("no")) {
                return false;
            }
            if (value.equalsIgnoreCase("y") || value.equalsIgnoreCase("yes")) {
                return true;
            }
            System.out.println("  Enter y or n.");
        }
    }

    public static final class InputClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
