package com.sebastian.mortgage;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The terms of a fixed-rate loan.
 *
 * @param principal          amount borrowed, greater than zero
 * @param annualRatePercent  nominal yearly interest rate in percent (4.5 means 4.5%), 0 or more
 * @param years              length of the loan in years, 1 to 50
 * @param extraMonthly       optional extra amount paid towards the principal every month, 0 or more
 */
public record Loan(BigDecimal principal, BigDecimal annualRatePercent, int years, BigDecimal extraMonthly) {

    public static final int MAX_YEARS = 50;
    public static final BigDecimal MAX_RATE_PERCENT = BigDecimal.valueOf(100);

    public Loan {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(annualRatePercent, "annualRatePercent");
        extraMonthly = extraMonthly == null ? BigDecimal.ZERO : extraMonthly;
        if (principal.signum() <= 0)
            throw new IllegalArgumentException("The principal must be greater than 0.");
        if (annualRatePercent.signum() < 0 || annualRatePercent.compareTo(MAX_RATE_PERCENT) > 0)
            throw new IllegalArgumentException("The interest rate must be between 0 and " + MAX_RATE_PERCENT + "%.");
        if (years < 1 || years > MAX_YEARS)
            throw new IllegalArgumentException("The period must be between 1 and " + MAX_YEARS + " years.");
        if (extraMonthly.signum() < 0)
            throw new IllegalArgumentException("The extra monthly payment cannot be negative.");
    }

    public Loan(BigDecimal principal, BigDecimal annualRatePercent, int years) {
        this(principal, annualRatePercent, years, BigDecimal.ZERO);
    }

    public int numberOfPayments() {
        return years * 12;
    }
}
