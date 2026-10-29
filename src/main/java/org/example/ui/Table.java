package org.example.ui;

import java.util.List;

final class Table {
    private Table() {
    }

    static void print(List<String> headers, List<List<String>> rows) {
        int columns = headers.size();
        int[] widths = new int[columns];
        for (int i = 0; i < columns; i++) {
            widths[i] = headers.get(i).length();
        }
        for (List<String> row : rows) {
            for (int i = 0; i < columns; i++) {
                widths[i] = Math.max(widths[i], cell(row, i).length());
            }
        }

        printLine(headers, widths);
        StringBuilder divider = new StringBuilder("  ");
        for (int i = 0; i < columns; i++) {
            if (i > 0) {
                divider.append("-+-");
            }
            divider.append("-".repeat(widths[i]));
        }
        System.out.println(divider);
        for (List<String> row : rows) {
            printLine(row, widths);
        }
    }

    private static void printLine(List<String> cells, int[] widths) {
        StringBuilder line = new StringBuilder("  ");
        for (int i = 0; i < widths.length; i++) {
            if (i > 0) {
                line.append(" | ");
            }
            String value = cell(cells, i);
            line.append(value).append(" ".repeat(Math.max(0, widths[i] - value.length())));
        }
        System.out.println(line);
    }

    private static String cell(List<String> cells, int index) {
        return index < cells.size() && cells.get(index) != null ? cells.get(index) : "";
    }
}
