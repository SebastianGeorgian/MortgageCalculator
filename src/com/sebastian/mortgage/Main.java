package com.sebastian.mortgage;

import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mortgage Calculator: monthly payment, totals and a full payment schedule for a fixed-rate loan.
 *
 * <p>Run without arguments for interactive mode, or pass the loan on the command line, see {@link #USAGE}.
 */
public final class Main {

    static final String USAGE = """
            Usage: mortgage [options]

            Without options the calculator asks for every value interactively.

              -p, --principal AMOUNT   amount borrowed, e.g. 300000
              -r, --rate PERCENT       annual interest rate, e.g. 4.5
              -y, --years N            loan period in years (1-50)
              -e, --extra AMOUNT       extra amount paid every month (default 0)
              -s, --schedule MODE      payment schedule to print: yearly (default), monthly or none
                  --csv FILE           also write the monthly schedule to FILE as CSV
              -h, --help               show this help

            Example: mortgage -p 300000 -r 4.5 -y 30 -e 200 --csv schedule.csv
            """;

    private static final BigDecimal MAX_PRINCIPAL = new BigDecimal("1000000000");

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        Options options;
        try {
            options = args.length == 0 ? interactive(out) : Options.parse(args);
        } catch (Console.EndOfInputException e) {
            out.println();
            return 0;
        } catch (IllegalArgumentException e) {
            err.println("mortgage: " + e.getMessage());
            err.println("Run with --help for usage.");
            return 2;
        }
        if (options.help) {
            out.print(USAGE);
            return 0;
        }

        Loan loan;
        try {
            loan = new Loan(options.principal, options.rate, options.years, options.extra);
        } catch (IllegalArgumentException e) {
            err.println("mortgage: " + e.getMessage());
            return 2;
        }
        var calculator = new MortgageCalculator(loan);
        var report = new MortgageReport(calculator);

        report.printSummary(out);
        switch (options.schedule) {
            case "monthly" -> report.printMonthlySchedule(out);
            case "yearly" -> report.printYearlySchedule(out);
            default -> { }
        }
        if (options.csv != null) {
            try (var writer = Files.newBufferedWriter(options.csv, StandardCharsets.UTF_8)) {
                report.writeCsv(writer);
            } catch (IOException e) {
                err.println("mortgage: cannot write " + options.csv + ": " + e.getMessage());
                return 1;
            }
            out.println();
            out.println("Monthly schedule written to " + options.csv);
        }
        return 0;
    }

    private static Options interactive(PrintStream out) {
        var console = new Console();
        var o = new Options();
        out.println("Mortgage Calculator (press Ctrl+D to quit)");
        out.println();
        o.principal = console.readDecimal("Principal", BigDecimal.ONE, MAX_PRINCIPAL, null);
        o.rate = console.readDecimal("Annual interest rate (%)", BigDecimal.ZERO, Loan.MAX_RATE_PERCENT, null);
        o.years = console.readInt("Period (years)", 1, Loan.MAX_YEARS);
        o.extra = console.readDecimal("Extra payment each month", BigDecimal.ZERO, MAX_PRINCIPAL, BigDecimal.ZERO);
        o.schedule = console.readYesNo("Show the schedule month by month instead of yearly?", false) ? "monthly" : "yearly";
        return o;
    }

    /** Command-line options. */
    static final class Options {
        BigDecimal principal;
        BigDecimal rate;
        int years;
        BigDecimal extra = BigDecimal.ZERO;
        String schedule = "yearly";
        Path csv;
        boolean help;

        static Options parse(String[] args) {
            var o = new Options();
            boolean haveYears = false;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                String value = null;
                int eq = arg.indexOf('=');
                if (arg.startsWith("--") && eq > 0) {
                    value = arg.substring(eq + 1);
                    arg = arg.substring(0, eq);
                }
                if (arg.equals("-h") || arg.equals("--help")) {
                    o.help = true;
                    return o;
                }
                if (value == null) {
                    if (i + 1 >= args.length)
                        throw new IllegalArgumentException("option " + arg + " needs a value");
                    value = args[++i];
                }
                switch (arg) {
                    case "-p", "--principal" -> o.principal = number(arg, value);
                    case "-r", "--rate" -> o.rate = number(arg, value);
                    case "-y", "--years" -> {
                        try {
                            o.years = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("--years must be a whole number, got '" + value + "'");
                        }
                        haveYears = true;
                    }
                    case "-e", "--extra" -> o.extra = number(arg, value);
                    case "-s", "--schedule" -> {
                        if (!value.matches("monthly|yearly|none"))
                            throw new IllegalArgumentException("--schedule must be monthly, yearly or none");
                        o.schedule = value;
                    }
                    case "--csv" -> o.csv = Path.of(value);
                    default -> throw new IllegalArgumentException("unknown option " + arg);
                }
            }
            if (o.principal == null || o.rate == null || !haveYears)
                throw new IllegalArgumentException("--principal, --rate and --years are all required");
            return o;
        }

        private static BigDecimal number(String option, String value) {
            try {
                return Console.parseDecimal(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(option + " must be a number, got '" + value + "'");
            }
        }
    }
}
