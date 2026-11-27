package com.sebastian.mortgage;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Formats a {@link MortgageCalculator} result as a text report or a CSV file. */
public final class MortgageReport {

    private final MortgageCalculator calculator;
    private final NumberFormat money;

    public MortgageReport(MortgageCalculator calculator, Locale locale) {
        this.calculator = calculator;
        this.money = NumberFormat.getNumberInstance(locale);
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
    }

    public MortgageReport(MortgageCalculator calculator) {
        this(calculator, Locale.getDefault(Locale.Category.FORMAT));
    }

    public void printSummary(PrintStream out) {
        Loan loan = calculator.loan();
        out.println();
        out.println("MORTGAGE");
        out.println("--------");
        out.printf("Principal:             %s%n", format(loan.principal()));
        out.printf("Annual interest rate:  %s%%%n", loan.annualRatePercent().stripTrailingZeros().toPlainString());
        out.printf("Period:                %d years (%d payments)%n", loan.years(), loan.numberOfPayments());
        out.printf("Monthly payment:       %s%n", format(calculator.monthlyPayment()));
        if (loan.extraMonthly().signum() > 0) {
            out.printf("Extra each month:      %s%n", format(loan.extraMonthly()));
            out.printf("Paid off after:        %s (instead of %d years)%n",
                    duration(calculator.monthsToPayOff()), loan.years());
            out.printf("Interest saved:        %s%n", format(calculator.interestSavedByExtra()));
        }
        out.printf("Total interest:        %s%n", format(calculator.totalInterest()));
        out.printf("Total paid:            %s%n", format(calculator.totalPaid()));
    }

    /** One row per month. */
    public void printMonthlySchedule(PrintStream out) {
        printTableHeader(out, "Month");
        for (Payment p : calculator.schedule())
            printRow(out, String.valueOf(p.month()), p.amount(), p.interest(), p.principal(), p.balance());
    }

    /** One row per year, summing that year's payments. */
    public void printYearlySchedule(PrintStream out) {
        printTableHeader(out, "Year");
        List<Payment> schedule = calculator.schedule();
        for (int start = 0; start < schedule.size(); start += 12) {
            List<Payment> year = schedule.subList(start, Math.min(start + 12, schedule.size()));
            printRow(out, String.valueOf(start / 12 + 1),
                    sum(year, Payment::amount), sum(year, Payment::interest), sum(year, Payment::principal),
                    year.get(year.size() - 1).balance());
        }
    }

    /** Machine-readable schedule: plain numbers with a dot as the decimal separator. */
    public void writeCsv(Writer out) throws IOException {
        out.write("month,payment,interest,principal,balance\n");
        for (Payment p : calculator.schedule())
            out.write(p.month() + "," + p.amount().toPlainString() + "," + p.interest().toPlainString() + ","
                    + p.principal().toPlainString() + "," + p.balance().toPlainString() + "\n");
    }

    private void printTableHeader(PrintStream out, String period) {
        out.println();
        out.println("PAYMENT SCHEDULE");
        out.println("----------------");
        out.printf("%6s %15s %15s %15s %16s%n", period, "Payment", "Interest", "Principal", "Balance");
    }

    private void printRow(PrintStream out, String period, BigDecimal payment, BigDecimal interest,
                          BigDecimal principal, BigDecimal balance) {
        out.printf("%6s %15s %15s %15s %16s%n", period, format(payment), format(interest), format(principal),
                format(balance));
    }

    private static BigDecimal sum(List<Payment> payments, java.util.function.Function<Payment, BigDecimal> field) {
        return payments.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String format(BigDecimal amount) {
        return money.format(amount);
    }

    static String duration(int months) {
        int years = months / 12, rest = months % 12;
        String y = years == 1 ? "1 year" : years + " years";
        String m = rest == 1 ? "1 month" : rest + " months";
        return years == 0 ? m : rest == 0 ? y : y + " " + m;
    }
}
