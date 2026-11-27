package com.sebastian.mortgage;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Computes the monthly payment and the full amortization schedule of a fixed-rate loan.
 *
 * <p>The payment uses the standard annuity formula
 * {@code M = P * r * (1 + r)^n / ((1 + r)^n - 1)}, where {@code r} is the monthly rate and
 * {@code n} the number of payments. Money is kept in {@link BigDecimal} and every
 * month is rounded to cents, the way a bank statement is; the final payment is adjusted so the
 * balance ends at exactly zero.
 */
public final class MortgageCalculator {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal TWELVE_HUNDRED = BigDecimal.valueOf(1200);

    private final Loan loan;
    private final List<Payment> schedule;

    public MortgageCalculator(Loan loan) {
        this.loan = loan;
        this.schedule = Collections.unmodifiableList(buildSchedule());
    }

    public Loan loan() {
        return loan;
    }

    /** The regular monthly payment, excluding any extra payment, rounded to cents. */
    public BigDecimal monthlyPayment() {
        return exactMonthlyPayment().setScale(2, RoundingMode.HALF_UP);
    }

    /** Every payment until the loan is repaid (fewer than years * 12 when extra payments are made). */
    public List<Payment> schedule() {
        return schedule;
    }

    public BigDecimal totalPaid() {
        return schedule.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalInterest() {
        return schedule.stream().map(Payment::interest).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Number of months until the balance reaches zero. */
    public int monthsToPayOff() {
        return schedule.size();
    }

    /** Interest saved by the extra monthly payment, compared with the same loan without it. */
    public BigDecimal interestSavedByExtra() {
        if (loan.extraMonthly().signum() == 0)
            return BigDecimal.ZERO;
        var withoutExtra = new MortgageCalculator(new Loan(loan.principal(), loan.annualRatePercent(), loan.years()));
        return withoutExtra.totalInterest().subtract(totalInterest());
    }

    /** Balance still owed after the given number of payments (the principal for 0). */
    public BigDecimal balanceAfter(int paymentsMade) {
        if (paymentsMade <= 0)
            return money(loan.principal());
        if (paymentsMade >= schedule.size())
            return BigDecimal.ZERO.setScale(2);
        return schedule.get(paymentsMade - 1).balance();
    }

    private BigDecimal monthlyRate() {
        return loan.annualRatePercent().divide(TWELVE_HUNDRED, MC);
    }

    private BigDecimal exactMonthlyPayment() {
        BigDecimal r = monthlyRate();
        int n = loan.numberOfPayments();
        if (r.signum() == 0)
            return loan.principal().divide(BigDecimal.valueOf(n), MC);
        BigDecimal growth = BigDecimal.ONE.add(r).pow(n, MC);
        return loan.principal().multiply(r).multiply(growth).divide(growth.subtract(BigDecimal.ONE), MC);
    }

    private List<Payment> buildSchedule() {
        BigDecimal rate = monthlyRate();
        BigDecimal regular = monthlyPayment().add(money(loan.extraMonthly()));
        BigDecimal balance = money(loan.principal());
        var payments = new ArrayList<Payment>(loan.numberOfPayments());

        for (int month = 1; balance.signum() > 0 && month <= loan.numberOfPayments(); month++) {
            BigDecimal interest = money(balance.multiply(rate, MC));
            BigDecimal principalPart = regular.subtract(interest);
            boolean last = month == loan.numberOfPayments() || principalPart.compareTo(balance) >= 0;
            if (last)
                principalPart = balance; // the final payment clears whatever rounding left over
            balance = balance.subtract(principalPart);
            payments.add(new Payment(month, interest.add(principalPart), interest, principalPart, balance));
        }
        return payments;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
