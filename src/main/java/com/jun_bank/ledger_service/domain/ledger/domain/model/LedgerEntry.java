package com.jun_bank.ledger_service.domain.ledger.domain.model;

import com.jun_bank.ledger_service.domain.ledger.domain.exception.LedgerException;
import com.jun_bank.ledger_service.domain.ledger.domain.model.vo.LedgerEntryId;
import com.jun_bank.ledger_service.domain.ledger.domain.model.vo.Money;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 원장 엔트리 도메인 모델 (Immutable)
 * <p>
 * 모든 거래의 불변 기록을 담당합니다.
 * <strong>INSERT만 가능하며, UPDATE/DELETE는 금지됩니다.</strong>
 *
 * <h3>Append-only 원칙:</h3>
 * <ul>
 *   <li>한번 생성된 엔트리는 수정 불가</li>
 *   <li>삭제 대신 반대 엔트리 추가 (취소 처리)</li>
 *   <li>모든 변경 이력 영구 보존</li>
 * </ul>
 *
 * <h3>복식부기:</h3>
 * <p>
 * 모든 거래는 DEBIT과 CREDIT 쌍으로 기록됩니다.
 * transactionId로 그룹화하여 차변 합계 = 대변 합계 검증 가능.
 * </p>
 *
 * <h3>사용 예:</h3>
 * <pre>{@code
 * // A → B 50,000원 이체
 * LedgerEntry debitEntry = LedgerEntry.createBuilder()
 *     .accountNumber("110-1234-5678-90")  // B 계좌
 *     .transactionId("TRF-xxx")
 *     .entryType(EntryType.DEBIT)
 *     .category(TransactionCategory.TRANSFER_IN)
 *     .amount(Money.of(50000))
 *     .balanceAfter(Money.of(150000))
 *     .description("A로부터 이체")
 *     .build();
 *
 * LedgerEntry creditEntry = LedgerEntry.createBuilder()
 *     .accountNumber("110-9876-5432-10")  // A 계좌
 *     .transactionId("TRF-xxx")
 *     .entryType(EntryType.CREDIT)
 *     .category(TransactionCategory.TRANSFER_OUT)
 *     .amount(Money.of(50000))
 *     .balanceAfter(Money.of(100000))
 *     .description("B로 이체")
 *     .build();
 * }</pre>
 *
 * @see EntryType
 * @see TransactionCategory
 */
@Getter
public class LedgerEntry {

    // ========================================
    // 핵심 필드 (모두 불변)
    // ========================================

    /**
     * 원장 엔트리 ID
     */
    private LedgerEntryId entryId;

    /**
     * 원본 거래 ID (복식부기 그룹화용)
     */
    private String transactionId;

    /**
     * 계좌번호
     */
    private String accountNumber;

    /**
     * 엔트리 유형 (DEBIT/CREDIT)
     */
    private EntryType entryType;

    /**
     * 거래 금액
     */
    private Money amount;

    /**
     * 거래 후 잔액
     */
    private Money balanceAfter;

    /**
     * 거래 설명
     */
    private String description;

    /**
     * 거래 카테고리
     */
    private TransactionCategory category;

    /**
     * 참조 서비스 타입 (원본 서비스)
     */
    private String referenceType;

    /**
     * 참조 ID (원본 ID)
     */
    private String referenceId;

    /**
     * 생성 시간 (불변)
     */
    private LocalDateTime createdAt;

    private LedgerEntry() {}

    // ========================================
    // 생성 메서드
    // ========================================

    /**
     * 신규 엔트리 생성 빌더
     * <p>
     * 엔트리는 생성 후 변경 불가합니다.
     * </p>
     *
     * @return LedgerEntryCreateBuilder
     */
    public static LedgerEntryCreateBuilder createBuilder() {
        return new LedgerEntryCreateBuilder();
    }

    /**
     * DB 복원용 빌더
     *
     * @return LedgerEntryRestoreBuilder
     */
    public static LedgerEntryRestoreBuilder restoreBuilder() {
        return new LedgerEntryRestoreBuilder();
    }

    // ========================================
    // 상태 확인 메서드 (읽기만 가능)
    // ========================================

    /**
     * 신규 여부 확인
     */
    public boolean isNew() {
        return this.entryId == null;
    }

    /**
     * 차변 여부 확인
     */
    public boolean isDebit() {
        return this.entryType.isDebit();
    }

    /**
     * 대변 여부 확인
     */
    public boolean isCredit() {
        return this.entryType.isCredit();
    }

    /**
     * 잔액 증가 엔트리 여부 확인
     */
    public boolean increasesBalance() {
        return this.entryType.increasesBalance();
    }

    /**
     * 잔액 감소 엔트리 여부 확인
     */
    public boolean decreasesBalance() {
        return this.entryType.decreasesBalance();
    }

    // ========================================
    // 비즈니스 메서드는 없음 (Immutable)
    // 수정이 필요하면 새 엔트리 생성
    // ========================================

    // ========================================
    // Builder 클래스
    // ========================================

    public static class LedgerEntryCreateBuilder {
        private String transactionId;
        private String accountNumber;
        private EntryType entryType;
        private Money amount;
        private Money balanceAfter;
        private String description;
        private TransactionCategory category;
        private String referenceType;
        private String referenceId;

        public LedgerEntryCreateBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public LedgerEntryCreateBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public LedgerEntryCreateBuilder entryType(EntryType entryType) {
            this.entryType = entryType;
            return this;
        }

        public LedgerEntryCreateBuilder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public LedgerEntryCreateBuilder balanceAfter(Money balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }

        public LedgerEntryCreateBuilder description(String description) {
            this.description = description;
            return this;
        }

        public LedgerEntryCreateBuilder category(TransactionCategory category) {
            this.category = category;
            return this;
        }

        public LedgerEntryCreateBuilder referenceType(String referenceType) {
            this.referenceType = referenceType;
            return this;
        }

        public LedgerEntryCreateBuilder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public LedgerEntry build() {
            // 필수 필드 검증
            if (transactionId == null || transactionId.isBlank()) {
                throw LedgerException.requiredFieldMissing("transactionId");
            }
            if (accountNumber == null || accountNumber.isBlank()) {
                throw LedgerException.requiredFieldMissing("accountNumber");
            }
            if (entryType == null) {
                throw LedgerException.requiredFieldMissing("entryType");
            }
            if (amount == null || !amount.isPositive()) {
                throw LedgerException.invalidAmount(amount != null ? amount.amount() : null);
            }
            if (balanceAfter == null) {
                throw LedgerException.requiredFieldMissing("balanceAfter");
            }
            if (category == null) {
                throw LedgerException.requiredFieldMissing("category");
            }

            LedgerEntry entry = new LedgerEntry();
            entry.transactionId = this.transactionId;
            entry.accountNumber = this.accountNumber;
            entry.entryType = this.entryType;
            entry.amount = this.amount;
            entry.balanceAfter = this.balanceAfter;
            entry.description = this.description;
            entry.category = this.category;
            entry.referenceType = this.referenceType;
            entry.referenceId = this.referenceId;
            entry.createdAt = LocalDateTime.now();

            return entry;
        }
    }

    public static class LedgerEntryRestoreBuilder {
        private LedgerEntryId entryId;
        private String transactionId;
        private String accountNumber;
        private EntryType entryType;
        private Money amount;
        private Money balanceAfter;
        private String description;
        private TransactionCategory category;
        private String referenceType;
        private String referenceId;
        private LocalDateTime createdAt;

        public LedgerEntryRestoreBuilder entryId(LedgerEntryId entryId) { this.entryId = entryId; return this; }
        public LedgerEntryRestoreBuilder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public LedgerEntryRestoreBuilder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        public LedgerEntryRestoreBuilder entryType(EntryType entryType) { this.entryType = entryType; return this; }
        public LedgerEntryRestoreBuilder amount(Money amount) { this.amount = amount; return this; }
        public LedgerEntryRestoreBuilder balanceAfter(Money balanceAfter) { this.balanceAfter = balanceAfter; return this; }
        public LedgerEntryRestoreBuilder description(String description) { this.description = description; return this; }
        public LedgerEntryRestoreBuilder category(TransactionCategory category) { this.category = category; return this; }
        public LedgerEntryRestoreBuilder referenceType(String referenceType) { this.referenceType = referenceType; return this; }
        public LedgerEntryRestoreBuilder referenceId(String referenceId) { this.referenceId = referenceId; return this; }
        public LedgerEntryRestoreBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public LedgerEntry build() {
            LedgerEntry entry = new LedgerEntry();
            entry.entryId = this.entryId;
            entry.transactionId = this.transactionId;
            entry.accountNumber = this.accountNumber;
            entry.entryType = this.entryType;
            entry.amount = this.amount;
            entry.balanceAfter = this.balanceAfter;
            entry.description = this.description;
            entry.category = this.category;
            entry.referenceType = this.referenceType;
            entry.referenceId = this.referenceId;
            entry.createdAt = this.createdAt;
            return entry;
        }
    }
}