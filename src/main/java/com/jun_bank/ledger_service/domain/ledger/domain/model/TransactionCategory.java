package com.jun_bank.ledger_service.domain.ledger.domain.model;

/**
 * 거래 카테고리
 * <p>
 * 원장 기록의 거래 유형을 분류합니다.
 *
 * <h3>카테고리별 엔트리 유형:</h3>
 * <table border="1">
 *   <tr><th>카테고리</th><th>기본 EntryType</th><th>설명</th></tr>
 *   <tr><td>DEPOSIT</td><td>DEBIT</td><td>계좌에 입금 (잔액 증가)</td></tr>
 *   <tr><td>WITHDRAWAL</td><td>CREDIT</td><td>계좌에서 출금 (잔액 감소)</td></tr>
 *   <tr><td>TRANSFER_IN</td><td>DEBIT</td><td>이체로 받음 (잔액 증가)</td></tr>
 *   <tr><td>TRANSFER_OUT</td><td>CREDIT</td><td>이체로 보냄 (잔액 감소)</td></tr>
 *   <tr><td>PAYMENT</td><td>CREDIT</td><td>카드 결제 (잔액 감소)</td></tr>
 *   <tr><td>REFUND</td><td>DEBIT</td><td>환불 (잔액 증가)</td></tr>
 *   <tr><td>FEE</td><td>CREDIT</td><td>수수료 (잔액 감소)</td></tr>
 *   <tr><td>INTEREST</td><td>DEBIT</td><td>이자 (잔액 증가)</td></tr>
 * </table>
 *
 * @see LedgerEntry
 * @see EntryType
 */
public enum TransactionCategory {

    /**
     * 입금
     * <p>현금, 계좌 이체 등으로 계좌에 입금</p>
     */
    DEPOSIT("입금", EntryType.DEBIT),

    /**
     * 출금
     * <p>ATM 출금, 창구 출금 등</p>
     */
    WITHDRAWAL("출금", EntryType.CREDIT),

    /**
     * 이체 입금
     * <p>다른 계좌에서 이체로 받은 금액</p>
     */
    TRANSFER_IN("이체입금", EntryType.DEBIT),

    /**
     * 이체 출금
     * <p>다른 계좌로 이체한 금액</p>
     */
    TRANSFER_OUT("이체출금", EntryType.CREDIT),

    /**
     * 결제
     * <p>카드 결제, 자동이체 등</p>
     */
    PAYMENT("결제", EntryType.CREDIT),

    /**
     * 환불
     * <p>결제 취소로 인한 환불</p>
     */
    REFUND("환불", EntryType.DEBIT),

    /**
     * 수수료
     * <p>이체 수수료, 계좌 유지비 등</p>
     */
    FEE("수수료", EntryType.CREDIT),

    /**
     * 이자
     * <p>예금 이자, 적금 이자 등</p>
     */
    INTEREST("이자", EntryType.DEBIT);

    private final String description;
    private final EntryType defaultEntryType;  // 기본 엔트리 유형

    TransactionCategory(String description, EntryType defaultEntryType) {
        this.description = description;
        this.defaultEntryType = defaultEntryType;
    }

    /**
     * 카테고리 설명 반환
     *
     * @return 한글 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * 기본 엔트리 유형 반환
     * <p>
     * 해당 카테고리의 거래가 발생할 때 기본적으로 사용되는 엔트리 유형입니다.
     * </p>
     *
     * @return 기본 EntryType
     */
    public EntryType getDefaultEntryType() {
        return defaultEntryType;
    }

    /**
     * 잔액 증가 카테고리 여부 확인
     *
     * @return 잔액 증가 카테고리이면 true
     */
    public boolean isIncreasing() {
        return defaultEntryType.increasesBalance();
    }

    /**
     * 잔액 감소 카테고리 여부 확인
     *
     * @return 잔액 감소 카테고리이면 true
     */
    public boolean isDecreasing() {
        return defaultEntryType.decreasesBalance();
    }

    /**
     * 이체 관련 카테고리 여부 확인
     *
     * @return TRANSFER_IN 또는 TRANSFER_OUT이면 true
     */
    public boolean isTransfer() {
        return this == TRANSFER_IN || this == TRANSFER_OUT;
    }

    /**
     * 결제 관련 카테고리 여부 확인
     *
     * @return PAYMENT 또는 REFUND이면 true
     */
    public boolean isPaymentRelated() {
        return this == PAYMENT || this == REFUND;
    }

    /**
     * 시스템 생성 카테고리 여부 확인
     * <p>
     * 사용자 요청이 아닌 시스템에서 자동 생성되는 거래입니다.
     * </p>
     *
     * @return FEE 또는 INTEREST이면 true
     */
    public boolean isSystemGenerated() {
        return this == FEE || this == INTEREST;
    }
}