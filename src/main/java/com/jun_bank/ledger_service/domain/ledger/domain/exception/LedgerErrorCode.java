package com.jun_bank.ledger_service.domain.ledger.domain.exception;

import com.jun_bank.common_lib.exception.ErrorCode;

/**
 * 원장 도메인 에러 코드
 * <p>
 * 원장 기록 및 감사 로그 관련 에러를 정의합니다.
 *
 * <h3>에러 코드 체계:</h3>
 * <ul>
 *   <li>LDG_001~009: 유효성 검증 오류 (400)</li>
 *   <li>LDG_010~019: 조회 오류 (404)</li>
 *   <li>LDG_020~029: 불변성 위반 오류 (403)</li>
 *   <li>LDG_030~039: 정합성 오류 (500)</li>
 * </ul>
 *
 * @see LedgerException
 * @see ErrorCode
 */
public enum LedgerErrorCode implements ErrorCode {

    // ========================================
    // 유효성 검증 오류 (400 Bad Request)
    // ========================================

    /**
     * 유효하지 않은 원장 엔트리 ID 형식
     */
    INVALID_ENTRY_ID_FORMAT("LDG_001", "유효하지 않은 원장 엔트리 ID 형식입니다", 400),

    /**
     * 유효하지 않은 감사 로그 ID 형식
     */
    INVALID_AUDIT_LOG_ID_FORMAT("LDG_002", "유효하지 않은 감사 로그 ID 형식입니다", 400),

    /**
     * 유효하지 않은 금액
     */
    INVALID_AMOUNT("LDG_003", "유효하지 않은 금액입니다", 400),

    /**
     * 필수 필드 누락
     */
    REQUIRED_FIELD_MISSING("LDG_004", "필수 필드가 누락되었습니다", 400),

    /**
     * 유효하지 않은 계좌번호
     */
    INVALID_ACCOUNT_NUMBER("LDG_005", "유효하지 않은 계좌번호입니다", 400),

    // ========================================
    // 조회 오류 (404 Not Found)
    // ========================================

    /**
     * 원장 엔트리를 찾을 수 없음
     */
    ENTRY_NOT_FOUND("LDG_010", "원장 기록을 찾을 수 없습니다", 404),

    /**
     * 감사 로그를 찾을 수 없음
     */
    AUDIT_LOG_NOT_FOUND("LDG_011", "감사 로그를 찾을 수 없습니다", 404),

    // ========================================
    // 불변성 위반 오류 (403 Forbidden)
    // ========================================

    /**
     * 원장 수정 시도
     */
    IMMUTABLE_ENTRY_UPDATE("LDG_020", "원장 기록은 수정할 수 없습니다", 403),

    /**
     * 원장 삭제 시도
     */
    IMMUTABLE_ENTRY_DELETE("LDG_021", "원장 기록은 삭제할 수 없습니다", 403),

    /**
     * 감사 로그 수정 시도
     */
    IMMUTABLE_AUDIT_LOG_UPDATE("LDG_022", "감사 로그는 수정할 수 없습니다", 403),

    /**
     * 감사 로그 삭제 시도
     */
    IMMUTABLE_AUDIT_LOG_DELETE("LDG_023", "감사 로그는 삭제할 수 없습니다", 403),

    // ========================================
    // 정합성 오류 (500 Internal Server Error)
    // ========================================

    /**
     * 잔액 불일치 감지
     */
    BALANCE_MISMATCH("LDG_030", "잔액 불일치가 감지되었습니다", 500),

    /**
     * 복식부기 균형 오류
     */
    DOUBLE_ENTRY_IMBALANCE("LDG_031", "차변과 대변이 일치하지 않습니다", 500),

    /**
     * 중복 거래 감지
     */
    DUPLICATE_TRANSACTION("LDG_032", "중복된 거래가 감지되었습니다", 500);

    private final String code;
    private final String message;
    private final int status;

    LedgerErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }
}