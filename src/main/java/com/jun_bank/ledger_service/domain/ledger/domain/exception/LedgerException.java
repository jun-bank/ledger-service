package com.jun_bank.ledger_service.domain.ledger.domain.exception;

import com.jun_bank.common_lib.exception.BusinessException;

import java.math.BigDecimal;

/**
 * 원장 도메인 예외
 * <p>
 * 원장 기록 및 감사 로그 관련 예외를 처리합니다.
 *
 * @see LedgerErrorCode
 * @see BusinessException
 */
public class LedgerException extends BusinessException {

    public LedgerException(LedgerErrorCode errorCode) {
        super(errorCode);
    }

    public LedgerException(LedgerErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    // ========================================
    // 유효성 검증 관련 팩토리 메서드
    // ========================================

    public static LedgerException invalidEntryIdFormat(String id) {
        return new LedgerException(LedgerErrorCode.INVALID_ENTRY_ID_FORMAT, "id=" + id);
    }

    public static LedgerException invalidAuditLogIdFormat(String id) {
        return new LedgerException(LedgerErrorCode.INVALID_AUDIT_LOG_ID_FORMAT, "id=" + id);
    }

    public static LedgerException invalidAmount(BigDecimal amount) {
        return new LedgerException(LedgerErrorCode.INVALID_AMOUNT,
                "amount=" + (amount != null ? amount.toPlainString() : "null"));
    }

    public static LedgerException requiredFieldMissing(String fieldName) {
        return new LedgerException(LedgerErrorCode.REQUIRED_FIELD_MISSING, "field=" + fieldName);
    }

    public static LedgerException invalidAccountNumber(String accountNumber) {
        return new LedgerException(LedgerErrorCode.INVALID_ACCOUNT_NUMBER,
                "accountNumber=" + accountNumber);
    }

    // ========================================
    // 조회 관련 팩토리 메서드
    // ========================================

    public static LedgerException entryNotFound(String entryId) {
        return new LedgerException(LedgerErrorCode.ENTRY_NOT_FOUND, "entryId=" + entryId);
    }

    public static LedgerException auditLogNotFound(String auditLogId) {
        return new LedgerException(LedgerErrorCode.AUDIT_LOG_NOT_FOUND, "auditLogId=" + auditLogId);
    }

    // ========================================
    // 불변성 위반 관련 팩토리 메서드
    // ========================================

    public static LedgerException immutableEntryUpdate(String entryId) {
        return new LedgerException(LedgerErrorCode.IMMUTABLE_ENTRY_UPDATE, "entryId=" + entryId);
    }

    public static LedgerException immutableEntryDelete(String entryId) {
        return new LedgerException(LedgerErrorCode.IMMUTABLE_ENTRY_DELETE, "entryId=" + entryId);
    }

    public static LedgerException immutableAuditLogUpdate(String auditLogId) {
        return new LedgerException(LedgerErrorCode.IMMUTABLE_AUDIT_LOG_UPDATE, "auditLogId=" + auditLogId);
    }

    public static LedgerException immutableAuditLogDelete(String auditLogId) {
        return new LedgerException(LedgerErrorCode.IMMUTABLE_AUDIT_LOG_DELETE, "auditLogId=" + auditLogId);
    }

    // ========================================
    // 정합성 관련 팩토리 메서드
    // ========================================

    public static LedgerException balanceMismatch(String accountNumber,
                                                  BigDecimal accountBalance,
                                                  BigDecimal ledgerBalance) {
        return new LedgerException(LedgerErrorCode.BALANCE_MISMATCH,
                String.format("accountNumber=%s, accountBalance=%s, ledgerBalance=%s",
                        accountNumber,
                        accountBalance.toPlainString(),
                        ledgerBalance.toPlainString()));
    }

    public static LedgerException doubleEntryImbalance(String transactionId,
                                                       BigDecimal debitTotal,
                                                       BigDecimal creditTotal) {
        return new LedgerException(LedgerErrorCode.DOUBLE_ENTRY_IMBALANCE,
                String.format("transactionId=%s, debit=%s, credit=%s",
                        transactionId,
                        debitTotal.toPlainString(),
                        creditTotal.toPlainString()));
    }

    public static LedgerException duplicateTransaction(String transactionId) {
        return new LedgerException(LedgerErrorCode.DUPLICATE_TRANSACTION,
                "transactionId=" + transactionId);
    }
}