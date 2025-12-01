package com.jun_bank.ledger_service.domain.ledger.domain.model.vo;

import com.jun_bank.ledger_service.domain.ledger.domain.exception.LedgerException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * 금액 VO (Value Object) - Ledger Service
 * <p>
 * 원장 금액을 안전하게 다루기 위한 불변 객체입니다.
 *
 * @param amount 금액 (BigDecimal, 0 이상)
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    private static final int SCALE = 0;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null) {
            throw LedgerException.invalidAmount(null);
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw LedgerException.invalidAmount(amount);
        }
        amount = amount.setScale(SCALE, ROUNDING_MODE);
    }

    public static Money of(long amount) {
        if (amount == 0) {
            return ZERO;
        }
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO;
        }
        return new Money(amount);
    }

    public static Money of(String amount) {
        return of(new BigDecimal(amount));
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            // 원장에서는 음수 결과도 기록 가능 (잔액 불일치 감지 등)
            // 다만 Money는 0 이상만 허용하므로 예외
            throw LedgerException.invalidAmount(result);
        }
        return new Money(result);
    }

    /**
     * 음수 허용 빼기 (잔액 계산용)
     * <p>
     * 원장 잔액 계산 시 사용됩니다.
     * 결과가 음수일 수 있습니다 (오류 상황 감지).
     * </p>
     *
     * @param other 뺄 금액
     * @return 결과 BigDecimal (음수 가능)
     */
    public BigDecimal subtractRaw(Money other) {
        return this.amount.subtract(other.amount);
    }

    public String formatted() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.KOREA);
        return format.format(amount) + "원";
    }

    public long toLong() {
        return amount.longValue();
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}