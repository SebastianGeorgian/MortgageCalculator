package com.sebastian.mortgage;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Dependency-free unit tests; run with {@code ./build.sh test}. */
public final class MortgageCalculatorTest {

    private static int checks, failures;

    public static void main(String[] args) throws Exception {
        knownMonthlyPayments();
        scheduleIsConsistent();
        zeroInterest();
        extraPaymentShortensTheLoan();
        balanceAfter();
        invalidLoansAreRejected();
        parsesUserTypedNumbers();
        csvAndReport();
        commandLine();
        if (failures > 0) {
            System.err.printf("%d of %d checks FAILED%n", failures, checks);
            System.exit(1);
        }
        System.out.printf("All %d checks passed%n", checks);
    }

    private static void knownMonthlyPayments() {
        // reference values from standard amortization tables
        check(payment("200000", "6.5", 30).equals(new BigDecimal("1264.14")), "200k @ 6.5% / 30y");
        check(payment("300000", "4.5", 30).equals(new BigDecimal("1520.06")), "300k @ 4.5% / 30y");
        check(payment("100000", "5", 15).equals(new BigDecimal("790.79")), "100k @ 5% / 15y");
        check(payment("1000", "12", 1).equals(new BigDecimal("88.85")), "1000 @ 12% / 1y");
    }

    private static void scheduleIsConsistent() {
        var calc = calculator("300000", "4.5", 30, "0");
        List<Payment> schedule = calc.schedule();
        check(schedule.size() == 360, "360 payments");
        check(schedule.get(359).balance().signum() == 0, "ends at zero");
        BigDecimal principalRepaid = schedule.stream().map(Payment::principal).reduce(BigDecimal.ZERO, BigDecimal::add);
        check(principalRepaid.compareTo(new BigDecimal("300000")) == 0, "principal parts sum to the loan");
        check(calc.totalPaid().compareTo(calc.totalInterest().add(new BigDecimal("300000"))) == 0, "total = principal + interest");
        check(schedule.get(0).interest().equals(new BigDecimal("1125.00")), "first month interest");
        for (Payment p : schedule)
            check(p.amount().compareTo(p.interest().add(p.principal())) == 0, "payment = interest + principal, month " + p.month());
        BigDecimal last = schedule.get(359).amount();
        // rounding the payment up to cents overpays a fraction of a cent each month, so the final
        // payment is slightly smaller than the regular one (1,516.71 instead of 1,520.06 here)
        check(last.compareTo(calc.monthlyPayment()) <= 0
                && last.compareTo(calc.monthlyPayment().multiply(new BigDecimal("0.99"))) > 0,
                "final payment is only a rounding adjustment");
    }

    private static void zeroInterest() {
        var calc = calculator("12000", "0", 1, "0");
        check(calc.monthlyPayment().equals(new BigDecimal("1000.00")), "0% splits the principal evenly");
        check(calc.totalInterest().signum() == 0, "0% has no interest");
        check(calc.schedule().size() == 12, "0% has 12 payments");
    }

    private static void extraPaymentShortensTheLoan() {
        var plain = calculator("200000", "6", 30, "0");
        var extra = calculator("200000", "6", 30, "200");
        check(extra.monthsToPayOff() < plain.monthsToPayOff(), "extra payment shortens the loan");
        check(extra.totalInterest().compareTo(plain.totalInterest()) < 0, "extra payment saves interest");
        check(extra.interestSavedByExtra().compareTo(plain.totalInterest().subtract(extra.totalInterest())) == 0, "interest saved");
        check(extra.schedule().get(extra.monthsToPayOff() - 1).balance().signum() == 0, "extra schedule ends at zero");
        var huge = calculator("1000", "5", 10, "5000");
        check(huge.monthsToPayOff() == 1, "an extra payment larger than the balance repays in one month");
        check(huge.totalPaid().compareTo(new BigDecimal("1004.17")) == 0, "one-month payoff includes one month of interest");
    }

    private static void balanceAfter() {
        var calc = calculator("100000", "5", 15, "0");
        check(calc.balanceAfter(0).equals(new BigDecimal("100000.00")), "balance before any payment");
        check(calc.balanceAfter(180).signum() == 0, "balance at the end");
        check(calc.balanceAfter(1).equals(calc.schedule().get(0).balance()), "balance after one payment");
    }

    private static void invalidLoansAreRejected() {
        rejects(() -> new Loan(BigDecimal.ZERO, BigDecimal.ONE, 10), "zero principal");
        rejects(() -> new Loan(BigDecimal.TEN, new BigDecimal("-1"), 10), "negative rate");
        rejects(() -> new Loan(BigDecimal.TEN, new BigDecimal("101"), 10), "rate above 100%");
        rejects(() -> new Loan(BigDecimal.TEN, BigDecimal.ONE, 0), "zero years");
        rejects(() -> new Loan(BigDecimal.TEN, BigDecimal.ONE, 51), "more than 50 years");
        rejects(() -> new Loan(BigDecimal.TEN, BigDecimal.ONE, 5, new BigDecimal("-5")), "negative extra");
    }

    private static void parsesUserTypedNumbers() {
        check(Console.parseDecimal("300000").equals(new BigDecimal("300000")), "plain");
        check(Console.parseDecimal("300 000").equals(new BigDecimal("300000")), "space grouping");
        check(Console.parseDecimal("300,000").equals(new BigDecimal("300000")), "comma grouping");
        check(Console.parseDecimal("1,250,000.50").equals(new BigDecimal("1250000.50")), "US style");
        check(Console.parseDecimal("1.250.000,50").equals(new BigDecimal("1250000.50")), "European style");
        check(Console.parseDecimal("4,5").equals(new BigDecimal("4.5")), "decimal comma");
        check(Console.parseDecimal("4.5").equals(new BigDecimal("4.5")), "decimal point");
        rejects(() -> Console.parseDecimal("abc"), "not a number");
    }

    private static void csvAndReport() throws Exception {
        var calc = calculator("12000", "0", 1, "0");
        var csv = new StringWriter();
        new MortgageReport(calc, Locale.US).writeCsv(csv);
        String[] lines = csv.toString().split("\n");
        check(lines.length == 13, "CSV has a header and 12 rows");
        check(lines[1].equals("1,1000.00,0.00,1000.00,11000.00"), "CSV row");
        check(lines[12].endsWith(",0.00"), "CSV ends at zero");

        String text = capture(out -> {
            var report = new MortgageReport(calculator("300000", "4.5", 30, "0"), Locale.US);
            report.printSummary(out);
            report.printYearlySchedule(out);
        });
        check(text.contains("Monthly payment:       1,520.06"), "summary shows the payment");
        check(text.lines().filter(l -> l.matches("\\s+\\d+ .*")).count() == 30, "yearly schedule has 30 rows");
        check(MortgageReport.duration(14).equals("1 year 2 months"), "duration text");
        check(MortgageReport.duration(24).equals("2 years"), "duration whole years");
    }

    private static void commandLine() {
        var err = new ByteArrayOutputStream();
        String out = capture(o -> check(Main.run(new String[]{"-p", "200000", "-r", "6.5", "-y", "30", "-s", "none"},
                o, new PrintStream(err)) == 0, "CLI succeeds"));
        check(out.contains("Monthly payment:"), "CLI prints the summary");
        check(Main.run(new String[]{"--principal=1000", "--rate", "5"}, new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(err)) == 2, "missing --years is an error");
        check(Main.run(new String[]{"-p", "1000", "-r", "5", "-y", "99"}, new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(err)) == 2, "out-of-range years is an error");
        check(capture(o -> Main.run(new String[]{"--help"}, o, o)).contains("Usage:"), "help");
    }

    // ---- helpers ----------------------------------------------------------

    private static BigDecimal payment(String principal, String rate, int years) {
        return calculator(principal, rate, years, "0").monthlyPayment();
    }

    private static MortgageCalculator calculator(String principal, String rate, int years, String extra) {
        return new MortgageCalculator(new Loan(new BigDecimal(principal), new BigDecimal(rate), years, new BigDecimal(extra)));
    }

    private static String capture(java.util.function.Consumer<PrintStream> body) {
        var buffer = new ByteArrayOutputStream();
        body.accept(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static void rejects(Runnable action, String what) {
        try {
            action.run();
            check(false, what + " should be rejected");
        } catch (IllegalArgumentException expected) {
            check(true, what);
        }
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            failures++;
            System.err.println("FAILED: " + what);
        }
    }
}
