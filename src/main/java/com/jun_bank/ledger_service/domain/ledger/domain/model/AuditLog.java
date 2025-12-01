package com.jun_bank.ledger_service.domain.ledger.domain.model;

import com.jun_bank.ledger_service.domain.ledger.domain.exception.LedgerException;
import com.jun_bank.ledger_service.domain.ledger.domain.model.vo.AuditLogId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 감사 로그 도메인 모델 (Immutable)
 * <p>
 * 시스템 전체의 감사 로그를 기록합니다.
 * <strong>INSERT만 가능하며, UPDATE/DELETE는 금지됩니다.</strong>
 *
 * <h3>기록 대상:</h3>
 * <ul>
 *   <li>로그인 성공/실패</li>
 *   <li>계좌 생성/수정/삭제</li>
 *   <li>잔액 변경</li>
 *   <li>카드 발급/상태 변경</li>
 *   <li>관리자 작업</li>
 * </ul>
 *
 * <h3>보안/법적 요구사항:</h3>
 * <ul>
 *   <li>금융 감사 대비 모든 작업 기록</li>
 *   <li>최소 5년 이상 보관</li>
 *   <li>변조 불가능한 형태로 저장</li>
 * </ul>
 *
 * @see LedgerEntry
 */
@Getter
public class AuditLog {

    // ========================================
    // 핵심 필드 (모두 불변)
    // ========================================

    /**
     * 감사 로그 ID
     */
    private AuditLogId auditLogId;

    /**
     * 이벤트 타입 (예: LOGIN_SUCCESS, BALANCE_CHANGED)
     */
    private String eventType;

    /**
     * 발생 서비스명
     */
    private String serviceName;

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 리소스 타입 (예: Account, Card)
     */
    private String resourceType;

    /**
     * 리소스 ID
     */
    private String resourceId;

    /**
     * 수행 액션 (예: CREATE, UPDATE, DELETE)
     */
    private String action;

    /**
     * 변경 전 값 (JSON)
     */
    private String previousValue;

    /**
     * 변경 후 값 (JSON)
     */
    private String newValue;

    /**
     * 클라이언트 IP
     */
    private String ipAddress;

    /**
     * User Agent
     */
    private String userAgent;

    /**
     * 추가 메타데이터 (JSON)
     */
    private String metadata;

    /**
     * 이벤트 발생 시간 (불변)
     */
    private LocalDateTime timestamp;

    private AuditLog() {}

    // ========================================
    // 생성 메서드
    // ========================================

    public static AuditLogCreateBuilder createBuilder() {
        return new AuditLogCreateBuilder();
    }

    public static AuditLogRestoreBuilder restoreBuilder() {
        return new AuditLogRestoreBuilder();
    }

    // ========================================
    // 상태 확인 메서드 (읽기만 가능)
    // ========================================

    public boolean isNew() {
        return this.auditLogId == null;
    }

    /**
     * 로그인 관련 이벤트 여부 확인
     */
    public boolean isLoginEvent() {
        return eventType != null && eventType.startsWith("LOGIN_");
    }

    /**
     * 데이터 변경 이벤트 여부 확인
     */
    public boolean isDataChangeEvent() {
        return action != null &&
                (action.equals("CREATE") || action.equals("UPDATE") || action.equals("DELETE"));
    }

    /**
     * 값 변경 여부 확인
     */
    public boolean hasValueChange() {
        return previousValue != null || newValue != null;
    }

    // ========================================
    // Builder 클래스
    // ========================================

    public static class AuditLogCreateBuilder {
        private String eventType;
        private String serviceName;
        private String userId;
        private String resourceType;
        private String resourceId;
        private String action;
        private String previousValue;
        private String newValue;
        private String ipAddress;
        private String userAgent;
        private String metadata;

        public AuditLogCreateBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public AuditLogCreateBuilder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public AuditLogCreateBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public AuditLogCreateBuilder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public AuditLogCreateBuilder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public AuditLogCreateBuilder action(String action) {
            this.action = action;
            return this;
        }

        public AuditLogCreateBuilder previousValue(String previousValue) {
            this.previousValue = previousValue;
            return this;
        }

        public AuditLogCreateBuilder newValue(String newValue) {
            this.newValue = newValue;
            return this;
        }

        public AuditLogCreateBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public AuditLogCreateBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public AuditLogCreateBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public AuditLog build() {
            // 필수 필드 검증
            if (eventType == null || eventType.isBlank()) {
                throw LedgerException.requiredFieldMissing("eventType");
            }
            if (serviceName == null || serviceName.isBlank()) {
                throw LedgerException.requiredFieldMissing("serviceName");
            }

            AuditLog log = new AuditLog();
            log.eventType = this.eventType;
            log.serviceName = this.serviceName;
            log.userId = this.userId;
            log.resourceType = this.resourceType;
            log.resourceId = this.resourceId;
            log.action = this.action;
            log.previousValue = this.previousValue;
            log.newValue = this.newValue;
            log.ipAddress = this.ipAddress;
            log.userAgent = this.userAgent;
            log.metadata = this.metadata;
            log.timestamp = LocalDateTime.now();

            return log;
        }
    }

    public static class AuditLogRestoreBuilder {
        private AuditLogId auditLogId;
        private String eventType;
        private String serviceName;
        private String userId;
        private String resourceType;
        private String resourceId;
        private String action;
        private String previousValue;
        private String newValue;
        private String ipAddress;
        private String userAgent;
        private String metadata;
        private LocalDateTime timestamp;

        public AuditLogRestoreBuilder auditLogId(AuditLogId auditLogId) { this.auditLogId = auditLogId; return this; }
        public AuditLogRestoreBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public AuditLogRestoreBuilder serviceName(String serviceName) { this.serviceName = serviceName; return this; }
        public AuditLogRestoreBuilder userId(String userId) { this.userId = userId; return this; }
        public AuditLogRestoreBuilder resourceType(String resourceType) { this.resourceType = resourceType; return this; }
        public AuditLogRestoreBuilder resourceId(String resourceId) { this.resourceId = resourceId; return this; }
        public AuditLogRestoreBuilder action(String action) { this.action = action; return this; }
        public AuditLogRestoreBuilder previousValue(String previousValue) { this.previousValue = previousValue; return this; }
        public AuditLogRestoreBuilder newValue(String newValue) { this.newValue = newValue; return this; }
        public AuditLogRestoreBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public AuditLogRestoreBuilder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public AuditLogRestoreBuilder metadata(String metadata) { this.metadata = metadata; return this; }
        public AuditLogRestoreBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public AuditLog build() {
            AuditLog log = new AuditLog();
            log.auditLogId = this.auditLogId;
            log.eventType = this.eventType;
            log.serviceName = this.serviceName;
            log.userId = this.userId;
            log.resourceType = this.resourceType;
            log.resourceId = this.resourceId;
            log.action = this.action;
            log.previousValue = this.previousValue;
            log.newValue = this.newValue;
            log.ipAddress = this.ipAddress;
            log.userAgent = this.userAgent;
            log.metadata = this.metadata;
            log.timestamp = this.timestamp;
            return log;
        }
    }
}