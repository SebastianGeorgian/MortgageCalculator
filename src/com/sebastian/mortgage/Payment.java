package com.sebastian.mortgage;

import java.math.BigDecimal;

/**
 * One monthly payment of the amortization schedule. All amounts are rounded to cents.
 *
 * @param month      1-based month number
 * @param amount     total paid this month (interest + principal)
 * @param interest   part of the payment that covers interest
 * @param principal  part of the payment that reduces the balance
 * @param balance    balance still owed after this payment
 */
public record Payment(int month, BigDecimal amount, BigDecimal interest, BigDecimal principal, BigDecimal balance) {
}
