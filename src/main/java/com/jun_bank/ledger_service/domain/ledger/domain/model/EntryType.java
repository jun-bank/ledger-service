package com.jun_bank.ledger_service.domain.ledger.domain.model;

/**
 * 원장 엔트리 유형
 * <p>
 * 복식부기 원칙에 따른 차변(Debit)과 대변(Credit)을 정의합니다.
 *
 * <h3>복식부기 원칙:</h3>
 * <ul>
 *   <li>모든 거래는 차변과 대변이 항상 균형</li>
 *   <li>차변 합계 = 대변 합계</li>
 * </ul>
 *
 * <h3>자산 계정 (은행 계좌):</h3>
 * <table border="1">
 *   <tr><th>유형</th><th>의미</th><th>잔액 영향</th></tr>
 *   <tr><td>DEBIT</td><td>자산 증가</td><td>잔액 증가 (입금)</td></tr>
 *   <tr><td>CREDIT</td><td>자산 감소</td><td>잔액 감소 (출금)</td></tr>
 * </table>
 *
 * <h3>이체 예시:</h3>
 * <pre>
 * A → B 50,000원 이체
 *
 * | 계좌 | DEBIT  | CREDIT | 설명      |
 * |------|--------|--------|-----------|
 * |  A   |   0    | 50,000 | 이체 출금 |
 * |  B   | 50,000 |   0    | 이체 입금 |
 *
 * 검증: DEBIT(50,000) = CREDIT(50,000) ✓
 * </pre>
 *
 * @see LedgerEntry
 */
public enum EntryType {

    /**
     * 차변 (Debit)
     * <p>
     * 자산 계정에서는 잔액 증가를 의미합니다.
     * 입금, 이체 입금, 환불 등에 사용됩니다.
     * </p>
     */
    DEBIT("차변", true),

    /**
     * 대변 (Credit)
     * <p>
     * 자산 계정에서는 잔액 감소를 의미합니다.
     * 출금, 이체 출금, 결제 등에 사용됩니다.
     * </p>
     */
    CREDIT("대변", false);

    private final String description;
    private final boolean increasesBalance;  // 자산 계정 기준 잔액 증가 여부

    EntryType(String description, boolean increasesBalance) {
        this.description = description;
        this.increasesBalance = increasesBalance;
    }

    /**
     * 유형 설명 반환
     *
     * @return 한글 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * 잔액 증가 여부 (자산 계정 기준)
     *
     * @return 잔액 증가이면 true
     */
    public boolean increasesBalance() {
        return increasesBalance;
    }

    /**
     * 잔액 감소 여부 (자산 계정 기준)
     *
     * @return 잔액 감소이면 true
     */
    public boolean decreasesBalance() {
        return !increasesBalance;
    }

    /**
     * 차변 여부 확인
     *
     * @return DEBIT이면 true
     */
    public boolean isDebit() {
        return this == DEBIT;
    }

    /**
     * 대변 여부 확인
     *
     * @return CREDIT이면 true
     */
    public boolean isCredit() {
        return this == CREDIT;
    }

    /**
     * 반대 유형 반환
     * <p>
     * 복식부기에서 대응 엔트리 생성 시 사용합니다.
     * </p>
     *
     * @return 반대 유형 (DEBIT ↔ CREDIT)
     */
    public EntryType opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}