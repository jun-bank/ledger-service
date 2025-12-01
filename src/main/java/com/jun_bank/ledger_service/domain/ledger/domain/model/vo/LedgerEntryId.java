package com.jun_bank.ledger_service.domain.ledger.domain.model.vo;

import com.jun_bank.common_lib.util.UuidUtils;
import com.jun_bank.ledger_service.domain.ledger.domain.exception.LedgerException;

/**
 * 원장 엔트리 식별자 VO (Value Object)
 * <p>
 * 원장 엔트리의 고유 식별자입니다.
 *
 * <h3>ID 형식:</h3>
 * <pre>LDG-xxxxxxxx (예: LDG-a1b2c3d4)</pre>
 *
 * @param value 원장 엔트리 ID 문자열 (LDG-xxxxxxxx 형식)
 */
public record LedgerEntryId(String value) {

    public static final String PREFIX = "LDG";

    public LedgerEntryId {
        if (!UuidUtils.isValidDomainId(value, PREFIX)) {
            throw LedgerException.invalidEntryIdFormat(value);
        }
    }

    public static LedgerEntryId of(String value) {
        return new LedgerEntryId(value);
    }

    public static String generateId() {
        return UuidUtils.generateDomainId(PREFIX);
    }
}