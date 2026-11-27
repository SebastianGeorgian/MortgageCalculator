# Mortgage Calculator

A command-line mortgage calculator in Java. It works out the monthly payment
for a fixed-rate loan, total interest and total cost, and a full amortization
schedule showing how much of each payment goes to interest and how much to principal.

- Exact money arithmetic with `BigDecimal`: every month is rounded to cents like a
  bank statement, and the final payment clears the balance to exactly 0.00
- Optional **extra monthly payment**, which shows how much sooner the loan is repaid and how much interest it saves
- Yearly or monthly schedule, plus **CSV export** for spreadsheets
- Interactive mode, or every value as a command-line option for scripting
- Accepts amounts typed as `300000`, `300 000`, `300,000` or `4,5`; no external dependencies

## Requirements

JDK 17 or newer (`javac` and `java`).

## Build and run

```sh
./build.sh                 # compiles and creates build/mortgage-calculator.jar
./build.sh test            # runs the unit tests
java -jar build/mortgage-calculator.jar            # interactive mode
java -jar build/mortgage-calculator.jar --help
```

## Example

```
$ java -jar build/mortgage-calculator.jar -p 200000 -r 6 -y 30 -e 200 --csv schedule.csv

MORTGAGE
--------
Principal:             200,000.00
Annual interest rate:  6%
Period:                30 years (360 payments)
Monthly payment:       1,199.10
Extra each month:      200.00
Paid off after:        21 years (instead of 30 years)
Interest saved:        79,800.86
Total interest:        151,876.18
Total paid:            351,876.18

PAYMENT SCHEDULE
----------------
  Year         Payment        Interest       Principal          Balance
     1       16,789.20       11,866.07        4,923.13       195,076.87
     2       16,789.20       11,562.45        5,226.75       189,850.12
   ...
```

## Options

| Option | Meaning |
|---|---|
| `-p`, `--principal AMOUNT` | amount borrowed |
| `-r`, `--rate PERCENT` | annual interest rate (0–100) |
| `-y`, `--years N` | loan period in years (1–50) |
| `-e`, `--extra AMOUNT` | extra amount paid every month (default 0) |
| `-s`, `--schedule MODE` | `yearly` (default), `monthly` or `none` |
| `--csv FILE` | also write the monthly schedule as CSV |

## How it works

The monthly payment is the standard annuity formula

    M = P · r · (1 + r)ⁿ / ((1 + r)ⁿ − 1)

where *P* is the principal, *r* the monthly rate (annual rate / 12) and *n* the number of
payments. At 0% interest the payment is simply P / n. Each month the interest is charged on
the remaining balance and the rest of the payment reduces it.

## Project layout

```
src/com/sebastian/mortgage/
  Main.java                 command-line and interactive entry point
  Loan.java                 loan terms, with validation
  MortgageCalculator.java   payment formula and amortization schedule
  Payment.java              one row of the schedule
  MortgageReport.java       text report and CSV export
  Console.java              validated keyboard input
test/com/sebastian/mortgage/
  MortgageCalculatorTest.java
build.sh
```

## License

[MIT](LICENSE)
