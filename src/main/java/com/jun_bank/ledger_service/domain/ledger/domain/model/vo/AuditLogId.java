package com.jun_bank.ledger_service.domain.ledger.domain.model.vo;

import com.jun_bank.common_lib.util.UuidUtils;
import com.jun_bank.ledger_service.domain.ledger.domain.exception.LedgerException;

/**
 * 감사 로그 식별자 VO (Value Object)
 * <p>
 * 감사 로그의 고유 식별자입니다.
 *
 * <h3>ID 형식:</h3>
 * <pre>AUD-xxxxxxxx (예: AUD-a1b2c3d4)</pre>
 *
 * @param value 감사 로그 ID 문자열 (AUD-xxxxxxxx 형식)
 */
public record AuditLogId(String value) {

    public static final String PREFIX = "AUD";

    public AuditLogId {
        if (!UuidUtils.isValidDomainId(value, PREFIX)) {
            throw LedgerException.invalidAuditLogIdFormat(value);
        }
    }

    public static AuditLogId of(String value) {
        return new AuditLogId(value);
    }

    public static String generateId() {
        return UuidUtils.generateDomainId(PREFIX);
    }
}