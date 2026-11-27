package com.sebastian.mortgage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/** Reads validated values from standard input, asking again until the answer is acceptable. */
public final class Console {

    /** Thrown when the user closes standard input (Ctrl+D) instead of answering. */
    public static final class EndOfInputException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public EndOfInputException() {
            super("No more input");
        }
    }

    private final BufferedReader in;
    private final PrintStream out;

    public Console() {
        this(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), System.out);
    }

    public Console(BufferedReader in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    /** A decimal number in [min, max]; an empty answer selects defaultValue when it is not null. */
    public BigDecimal readDecimal(String prompt, BigDecimal min, BigDecimal max, BigDecimal defaultValue) {
        while (true) {
            String answer = readLine(prompt + (defaultValue != null ? " [" + defaultValue.toPlainString() + "]" : "") + ": ");
            if (answer.isEmpty() && defaultValue != null)
                return defaultValue;
            try {
                BigDecimal value = parseDecimal(answer);
                if (value.compareTo(min) >= 0 && value.compareTo(max) <= 0)
                    return value;
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            out.println("  Please enter a number between " + min.toPlainString() + " and " + max.toPlainString() + ".");
        }
    }

    /** A whole number in [min, max]. */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String answer = readLine(prompt + ": ");
            try {
                int value = Integer.parseInt(answer);
                if (value >= min && value <= max)
                    return value;
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            out.println("  Please enter a whole number between " + min + " and " + max + ".");
        }
    }

    /** A yes/no question; an empty answer selects defaultValue. */
    public boolean readYesNo(String prompt, boolean defaultValue) {
        while (true) {
            String answer = readLine(prompt + (defaultValue ? " [Y/n]: " : " [y/N]: ")).toLowerCase();
            if (answer.isEmpty())
                return defaultValue;
            if (answer.equals("y") || answer.equals("yes"))
                return true;
            if (answer.equals("n") || answer.equals("no"))
                return false;
            out.println("  Please answer y or n.");
        }
    }

    /**
     * Parses user-typed amounts: accepts spaces, underscores and apostrophes as digit grouping,
     * and either "." or "," as the decimal separator ("300 000", "300,000", "4,5" and "4.5").
     */
    static BigDecimal parseDecimal(String text) {
        String s = text.trim().replaceAll("[\\s_']", "");
        int dot = s.lastIndexOf('.'), comma = s.lastIndexOf(',');
        if (dot >= 0 && comma >= 0) {
            // both present: the last one is the decimal separator, the other groups digits
            s = dot > comma ? s.replace(",", "") : s.replace(".", "").replace(',', '.');
        } else if (comma >= 0) {
            // a single comma followed by exactly three digits is a thousands separator (300,000)
            boolean grouping = s.indexOf(',') != comma || s.length() - comma - 1 == 3;
            s = grouping ? s.replace(",", "") : s.replace(',', '.');
        }
        return new BigDecimal(s);
    }

    private String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            String line = in.readLine();
            if (line == null)
                throw new EndOfInputException();
            return line.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
