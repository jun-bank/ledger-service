# Ledger Service

> 원장 서비스 - 모든 거래의 불변 기록, 감사 로그

## 📋 개요

| 항목 | 내용 |
|------|------|
| 포트 | 8085 |
| 데이터베이스 | ledger_db (PostgreSQL) |
| 주요 역할 | 거래 원장 기록, 감사 로그, 잔액 검증 |

## 🎯 학습 포인트

### 1. Append-only 설계 ⭐ (핵심 학습 주제)

**Append-only란?**
> 데이터를 삽입(INSERT)만 하고, 수정(UPDATE)/삭제(DELETE)하지 않는 설계

**왜 필요한가?**
- **감사 추적**: 모든 변경 이력 보존
- **데이터 무결성**: 과거 데이터 변조 방지
- **법적 요구사항**: 금융 거래 기록 보존 의무
- **장애 복구**: 모든 이벤트를 재생하여 상태 복구 가능

```
┌─────────────────────────────────────────────────────────────┐
│                    일반 테이블 vs Append-only                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   일반 테이블 (Mutable)                                     │
│   ┌───────────────────────────────────────┐                 │
│   │ id │ account │ balance │ updated_at  │                 │
│   │  1 │ 110-xxx │ 100,000 │ 2024-01-01  │ ← 덮어씀       │
│   │  1 │ 110-xxx │ 150,000 │ 2024-01-02  │ ← 이전값 유실! │
│   └───────────────────────────────────────┘                 │
│                                                             │
│   ❌ 문제: 100,000 → 150,000 어떻게 변했는지 알 수 없음     │
│                                                             │
│   ─────────────────────────────────────────────────────     │
│                                                             │
│   Append-only 테이블 (Immutable)                            │
│   ┌──────────────────────────────────────────────────────┐  │
│   │ id │ account │ type    │ amount  │ balance │ time   │  │
│   │  1 │ 110-xxx │ DEPOSIT │ 100,000 │ 100,000 │ 01-01  │  │
│   │  2 │ 110-xxx │ DEPOSIT │  50,000 │ 150,000 │ 01-02  │  │
│   │  3 │ 110-xxx │ WITHDRAW│  30,000 │ 120,000 │ 01-03  │  │
│   └──────────────────────────────────────────────────────┘  │
│                                                             │
│   ✅ 장점: 모든 변경 이력 추적 가능                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2. 이벤트 소싱 (Event Sourcing) 개념

**현재 상태 = 모든 이벤트의 누적**

```
┌─────────────────────────────────────────────────────────────┐
│                    이벤트 소싱 예시                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   계좌 A의 현재 잔액을 구하려면?                             │
│                                                             │
│   이벤트 로그:                                              │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ 1. ACCOUNT_CREATED  | account: A | balance: 0      │   │
│   │ 2. DEPOSIT          | account: A | amount: +100,000│   │
│   │ 3. WITHDRAW         | account: A | amount: -30,000 │   │
│   │ 4. TRANSFER_IN      | account: A | amount: +50,000 │   │
│   │ 5. PAYMENT          | account: A | amount: -20,000 │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                             │
│   현재 잔액 = 0 + 100,000 - 30,000 + 50,000 - 20,000       │
│            = 100,000원                                      │
│                                                             │
│   ✅ 이벤트만 있으면 어느 시점의 잔액도 계산 가능!          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3. 복식부기 (Double-Entry Bookkeeping)

**모든 거래는 차변(Debit)과 대변(Credit)이 균형**

```
┌─────────────────────────────────────────────────────────────┐
│                    복식부기 예시: A → B 이체                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   A가 B에게 50,000원 이체                                   │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ entry │ account │ debit   │ credit  │ description  │   │
│   │   1   │    A    │    0    │ 50,000  │ 이체 출금    │   │
│   │   1   │    B    │ 50,000  │    0    │ 이체 입금    │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                             │
│   검증: 차변 합계 (50,000) = 대변 합계 (50,000) ✓           │
│                                                             │
│   ─────────────────────────────────────────────────────     │
│                                                             │
│   잔액 불일치 감지 예시                                     │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ entry │ account │ debit   │ credit  │ description  │   │
│   │   2   │    A    │ 30,000  │    0    │ ???          │   │
│   │   (대변 없음!)                                      │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                             │
│   ❌ 차변 ≠ 대변 → 데이터 무결성 위반 감지!                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4. 잔액 검증 (Reconciliation)

**Account Service 잔액 vs Ledger 계산 잔액 비교**

```java
@Scheduled(cron = "0 0 2 * * ?")  // 매일 새벽 2시
public void verifyBalances() {
    List<Account> accounts = accountRepository.findAll();
    
    for (Account account : accounts) {
        BigDecimal accountBalance = account.getBalance();
        BigDecimal ledgerBalance = ledgerRepository
            .calculateBalance(account.getAccountNumber());
        
        if (!accountBalance.equals(ledgerBalance)) {
            // 불일치 감지! 알림 발송
            alertService.sendBalanceMismatchAlert(
                account, accountBalance, ledgerBalance);
        }
    }
}
```

---

## 🗄️ 도메인 모델

### 도메인 구조
```
domain/ledger/domain/
├── exception/
│   ├── LedgerErrorCode.java        # 에러 코드 정의
│   └── LedgerException.java        # 도메인 예외
└── model/
    ├── LedgerEntry.java            # 원장 엔트리 (Immutable)
    ├── AuditLog.java               # 감사 로그 (Immutable)
    ├── EntryType.java              # DEBIT/CREDIT
    ├── TransactionCategory.java    # 거래 카테고리
    └── vo/
        ├── LedgerEntryId.java      # LDG-xxxxxxxx
        ├── AuditLogId.java         # AUD-xxxxxxxx
        └── Money.java              # 금액 VO
```

### LedgerEntry 도메인 모델 (Immutable)
```
┌─────────────────────────────────────────────────────────────┐
│                      LedgerEntry                             │
│                    ⚠️ INSERT만 허용!                          │
├─────────────────────────────────────────────────────────────┤
│ 【핵심 필드 - 모두 불변】                                      │
│ entryId: LedgerEntryId (PK, LDG-xxxxxxxx)                   │
│ transactionId: String (복식부기 그룹화용)                   │
│ accountNumber: String                                       │
│ entryType: EntryType (DEBIT/CREDIT)                        │
│ amount: Money (거래 금액)                                   │
│ balanceAfter: Money (거래 후 잔액)                          │
│ description: String (거래 설명)                             │
│ category: TransactionCategory                               │
│ referenceType: String (원본 서비스)                         │
│ referenceId: String (원본 ID)                               │
│ createdAt: LocalDateTime (불변)                             │
├─────────────────────────────────────────────────────────────┤
│ 【읽기 전용 메서드】                                          │
│ + isNew(), isDebit(), isCredit()                            │
│ + increasesBalance(), decreasesBalance()                    │
│                                                             │
│ 【비즈니스 메서드 없음 - Immutable】                         │
│ 수정 필요시 새 엔트리 추가 (취소 처리 등)                    │
└─────────────────────────────────────────────────────────────┘
```

### EntryType Enum (복식부기)
```java
public enum EntryType {
    DEBIT("차변", increasesBalance=true),   // 자산 증가 (입금)
    CREDIT("대변", increasesBalance=false); // 자산 감소 (출금)
    
    public boolean increasesBalance();      // 잔액 증가 여부
    public boolean decreasesBalance();      // 잔액 감소 여부
    public boolean isDebit();
    public boolean isCredit();
    public EntryType opposite();            // DEBIT ↔ CREDIT
}
```

### TransactionCategory Enum
```java
public enum TransactionCategory {
    DEPOSIT("입금", DEBIT),
    WITHDRAWAL("출금", CREDIT),
    TRANSFER_IN("이체입금", DEBIT),
    TRANSFER_OUT("이체출금", CREDIT),
    PAYMENT("결제", CREDIT),
    REFUND("환불", DEBIT),
    FEE("수수료", CREDIT),
    INTEREST("이자", DEBIT);
    
    public EntryType getDefaultEntryType();
    public boolean isIncreasing();
    public boolean isDecreasing();
    public boolean isTransfer();
    public boolean isPaymentRelated();
    public boolean isSystemGenerated();     // FEE, INTEREST
}
```

### AuditLog 도메인 모델 (Immutable)
```
┌─────────────────────────────────────────────────────────────┐
│                       AuditLog                               │
│                    ⚠️ INSERT만 허용!                          │
├─────────────────────────────────────────────────────────────┤
│ 【핵심 필드 - 모두 불변】                                      │
│ auditLogId: AuditLogId (PK, AUD-xxxxxxxx)                   │
│ eventType: String (LOGIN_SUCCESS, BALANCE_CHANGED 등)       │
│ serviceName: String (발생 서비스)                            │
│ userId: String                                              │
│ resourceType: String (예: "Account", "Card")                │
│ resourceId: String                                          │
│ action: String (CREATE/UPDATE/DELETE)                       │
│ previousValue: String (JSON)                                │
│ newValue: String (JSON)                                     │
│ ipAddress: String                                           │
│ userAgent: String                                           │
│ metadata: String (JSON)                                     │
│ timestamp: LocalDateTime (불변)                             │
├─────────────────────────────────────────────────────────────┤
│ 【읽기 전용 메서드】                                          │
│ + isNew(), isLoginEvent(), isDataChangeEvent()              │
│ + hasValueChange()                                          │
└─────────────────────────────────────────────────────────────┘
```

### Exception 체계

#### LedgerErrorCode
```java
public enum LedgerErrorCode implements ErrorCode {
    // 유효성 (400)
    INVALID_ENTRY_ID_FORMAT, INVALID_AUDIT_LOG_ID_FORMAT,
    INVALID_AMOUNT, REQUIRED_FIELD_MISSING, INVALID_ACCOUNT_NUMBER,
    
    // 조회 (404)
    ENTRY_NOT_FOUND, AUDIT_LOG_NOT_FOUND,
    
    // 불변성 위반 (403)
    IMMUTABLE_ENTRY_UPDATE, IMMUTABLE_ENTRY_DELETE,
    IMMUTABLE_AUDIT_LOG_UPDATE, IMMUTABLE_AUDIT_LOG_DELETE,
    
    // 정합성 (500)
    BALANCE_MISMATCH, DOUBLE_ENTRY_IMBALANCE, DUPLICATE_TRANSACTION;
}
```

#### LedgerException (팩토리 메서드)
```java
public class LedgerException extends BusinessException {
    public static LedgerException entryNotFound(String entryId);
    public static LedgerException immutableEntryUpdate(String entryId);
    public static LedgerException balanceMismatch(String accountNumber, BigDecimal accountBalance, BigDecimal ledgerBalance);
    public static LedgerException doubleEntryImbalance(String transactionId, BigDecimal debitTotal, BigDecimal creditTotal);
    // ...
}
```

---

## 📡 API 명세

### 1. 원장 기록 조회 (계좌별)
```http
GET /api/v1/ledger/entries?accountNumber=110-1234-5678-90&page=0&size=20
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "accountNumber": "110-1234-5678-90",
  "entries": [
    {
      "entryId": "LDG-uuid-1",
      "transactionId": "TXN-uuid-abcd",
      "entryType": "DEBIT",
      "amount": 100000,
      "balanceAfter": 250000,
      "description": "급여 입금",
      "category": "DEPOSIT",
      "createdAt": "2024-01-15T10:30:00"
    },
    {
      "entryId": "LDG-uuid-2",
      "transactionId": "TXN-uuid-efgh",
      "entryType": "CREDIT",
      "amount": 50000,
      "balanceAfter": 200000,
      "description": "ATM 출금",
      "category": "WITHDRAWAL",
      "createdAt": "2024-01-15T11:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100
}
```

### 2. 특정 시점 잔액 조회
```http
GET /api/v1/ledger/balance?accountNumber=110-1234-5678-90&asOf=2024-01-15T00:00:00
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "accountNumber": "110-1234-5678-90",
  "balance": 150000,
  "asOf": "2024-01-15T00:00:00",
  "entryCount": 45
}
```

### 3. 감사 로그 조회
```http
GET /api/v1/ledger/audit-logs?userId=USR-a1b2c3d4&page=0&size=20
X-User-Id: admin-user
X-User-Role: ADMIN
```

**Response (200 OK)**
```json
{
  "logs": [
    {
      "auditLogId": "AUD-uuid-1",
      "eventType": "LOGIN_SUCCESS",
      "serviceName": "auth-server",
      "userId": "USR-a1b2c3d4",
      "action": "LOGIN",
      "ipAddress": "192.168.1.100",
      "timestamp": "2024-01-15T09:00:00"
    },
    {
      "auditLogId": "AUD-uuid-2",
      "eventType": "BALANCE_CHANGED",
      "serviceName": "account-service",
      "userId": "USR-a1b2c3d4",
      "resourceType": "Account",
      "resourceId": "ACC-12345678",
      "action": "UPDATE",
      "previousValue": "{\"balance\": 100000}",
      "newValue": "{\"balance\": 150000}",
      "timestamp": "2024-01-15T10:30:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 200
}
```

---

## 📂 패키지 구조

```
com.jun_bank.ledger_service
├── LedgerServiceApplication.java
├── global/                              # 전역 설정 레이어
│   ├── config/                          # 설정 클래스
│   │   ├── JpaConfig.java               # JPA Auditing 활성화
│   │   ├── QueryDslConfig.java          # QueryDSL JPAQueryFactory 빈
│   │   ├── KafkaProducerConfig.java     # Kafka Producer (멱등성, JacksonJsonSerializer)
│   │   ├── KafkaConsumerConfig.java     # Kafka Consumer (수동 ACK, JacksonJsonDeserializer)
│   │   ├── SecurityConfig.java          # Spring Security (헤더 기반 인증)
│   │   ├── FeignConfig.java             # Feign Client 설정
│   │   ├── SwaggerConfig.java           # OpenAPI 문서화
│   │   └── AsyncConfig.java             # 비동기 처리 (ThreadPoolTaskExecutor)
│   ├── infrastructure/
│   │   ├── entity/
│   │   │   └── BaseEntity.java          # 공통 엔티티 (Audit, Soft Delete)
│   │   └── jpa/
│   │       └── AuditorAwareImpl.java    # JPA Auditing 사용자 정보
│   ├── security/
│   │   ├── UserPrincipal.java           # 인증 사용자 Principal
│   │   ├── HeaderAuthenticationFilter.java # Gateway 헤더 인증 필터
│   │   └── SecurityContextUtil.java     # SecurityContext 유틸리티
│   ├── feign/
│   │   ├── FeignErrorDecoder.java       # Feign 에러 → BusinessException 변환
│   │   └── FeignRequestInterceptor.java # 인증 헤더 전파
│   └── aop/
│       └── LoggingAspect.java           # 요청/응답 로깅 AOP
└── domain/
    └── ledger/                          # Ledger Bounded Context
        ├── domain/                      # 순수 도메인 ★ 구현 완료
        │   ├── exception/
        │   │   ├── LedgerErrorCode.java
        │   │   └── LedgerException.java
        │   └── model/
        │       ├── LedgerEntry.java         # Immutable
        │       ├── AuditLog.java            # Immutable
        │       ├── EntryType.java
        │       ├── TransactionCategory.java
        │       └── vo/
        │           ├── LedgerEntryId.java
        │           ├── AuditLogId.java
        │           └── Money.java
        ├── application/                 # 유스케이스 (TODO)
        │   ├── port/
        │   │   ├── in/
        │   │   └── out/
        │   ├── service/
        │   ├── dto/
        │   └── scheduler/               # 잔액 검증 스케줄러
        │       └── BalanceVerificationScheduler.java
        ├── infrastructure/              # Adapter Out (TODO)
        │   ├── persistence/
        │   │   ├── entity/              # JPA Entity
        │   │   ├── repository/
        │   │   └── adapter/
        │   ├── kafka/
        │   └── protection/              # 불변성 보호 (추후 구현)
        │       ├── AppendOnlyInterceptor.java
        │       └── ImmutableEntity.java
        └── presentation/                # Adapter In (TODO)
            ├── controller/
            └── dto/
```

---

## 🔧 Global 레이어 상세

### Config 설정

| 클래스 | 설명 |
|--------|------|
| `JpaConfig` | JPA Auditing 활성화 (`@EnableJpaAuditing`) |
| `QueryDslConfig` | `JPAQueryFactory` 빈 등록 |
| `KafkaProducerConfig` | 멱등성 Producer (ENABLE_IDEMPOTENCE=true, ACKS=all) |
| `KafkaConsumerConfig` | 수동 ACK (MANUAL_IMMEDIATE), group-id: ledger-service-group |
| `SecurityConfig` | Stateless 세션, 헤더 기반 인증, CSRF 비활성화 |
| `FeignConfig` | 로깅 레벨 BASIC, 에러 디코더, 요청 인터셉터 |
| `SwaggerConfig` | OpenAPI 3.0 문서화 설정 |
| `AsyncConfig` | ThreadPoolTaskExecutor (core=5, max=10, queue=25) |

### Security 설정

| 클래스 | 설명 |
|--------|------|
| `HeaderAuthenticationFilter` | `X-User-Id`, `X-User-Role`, `X-User-Email` 헤더 → SecurityContext |
| `UserPrincipal` | `UserDetails` 구현체, 인증된 사용자 정보 |
| `SecurityContextUtil` | 현재 사용자 조회 유틸리티 |

### BaseEntity (Soft Delete 지원)

```java
@MappedSuperclass
public abstract class BaseEntity {
    private LocalDateTime createdAt;      // 생성일시 (자동)
    private LocalDateTime updatedAt;      // 수정일시 (자동)
    private String createdBy;             // 생성자 (자동)
    private String updatedBy;             // 수정자 (자동)
    private LocalDateTime deletedAt;      // 삭제일시
    private String deletedBy;             // 삭제자
    private Boolean isDeleted = false;    // 삭제 여부
    
    public void delete(String deletedBy);  // Soft Delete
    public void restore();                 // 복구
}
```

### 추후 구현 예정 (불변성 보호)

| 클래스 | 설명 |
|--------|------|
| `AppendOnlyInterceptor` | UPDATE/DELETE 차단 인터셉터 |
| `ImmutableEntity` | 불변 엔티티 마커 인터페이스 |

---

## 🔗 서비스 간 통신

### 발행 이벤트 (Kafka Producer) - 최소화
| 이벤트 | 토픽 | 수신 서비스 | 설명 |
|--------|------|-------------|------|
| ENTRY_CREATED | ledger.entry.created | - | 기록 완료 확인 |
| BALANCE_MISMATCH | ledger.balance.mismatch | Alert | 불일치 알림 |

### 수신 이벤트 (Kafka Consumer) - 주요 역할
| 이벤트 | 토픽 | 발신 서비스 | 설명 |
|--------|------|-------------|------|
| BALANCE_CHANGED | account.balance.changed | Account | 잔액 변경 기록 |
| DEPOSIT_COMPLETED | transaction.deposit.completed | Transaction | 입금 기록 |
| WITHDRAWAL_COMPLETED | transaction.withdrawal.completed | Transaction | 출금 기록 |
| TRANSFER_COMPLETED | transfer.completed | Transfer | 이체 기록 |
| TRANSFER_FAILED | transfer.failed | Transfer | 이체 실패 기록 |
| PAYMENT_COMPLETED | card.payment.completed | Card | 결제 기록 |
| PAYMENT_CANCELLED | card.payment.cancelled | Card | 결제 취소 기록 |
| LOGIN_SUCCESS | auth.login.success | Auth | 로그인 감사 로그 |
| LOGIN_FAILED | auth.login.failed | Auth | 로그인 실패 로그 |

### Feign Client 호출
| 대상 서비스 | 용도 | 비고 |
|-------------|------|------|
| Account Service | 잔액 검증 | 정합성 확인 |

---

## ⚙️ Append-only 보호 설정

### 1. JPA 인터셉터로 UPDATE/DELETE 차단
```java
@Component
public class AppendOnlyInterceptor implements PreUpdateEventListener, PreDeleteEventListener {
    
    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        if (event.getEntity() instanceof ImmutableEntity) {
            throw new IllegalStateException(
                "UPDATE not allowed on immutable entity: " + 
                event.getEntity().getClass().getSimpleName());
        }
        return false;
    }
    
    @Override
    public boolean onPreDelete(PreDeleteEvent event) {
        if (event.getEntity() instanceof ImmutableEntity) {
            throw new IllegalStateException(
                "DELETE not allowed on immutable entity: " + 
                event.getEntity().getClass().getSimpleName());
        }
        return false;
    }
}
```

### 2. 마커 인터페이스
```java
public interface ImmutableEntity {
    // 이 인터페이스를 구현한 엔티티는 UPDATE/DELETE 불가
}

@Entity
public class LedgerEntry implements ImmutableEntity {
    // ...
}
```

### 3. DB 레벨 트리거 (추가 보호)
```sql
-- UPDATE 방지 트리거
CREATE OR REPLACE FUNCTION prevent_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'UPDATE not allowed on ledger_entries table';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER no_update_ledger
BEFORE UPDATE ON ledger_entries
FOR EACH ROW
EXECUTE FUNCTION prevent_update();

-- DELETE 방지 트리거
CREATE TRIGGER no_delete_ledger
BEFORE DELETE ON ledger_entries
FOR EACH ROW
EXECUTE FUNCTION prevent_delete();
```

---

## 🧪 테스트 시나리오

### 1. Append-only 보호 테스트
```java
@Test
void 원장_기록_수정_시도시_예외_발생() {
    // Given: 원장 기록 생성
    LedgerEntry entry = ledgerRepository.save(createEntry());
    
    // When & Then: 수정 시도 시 예외 발생
    entry.setAmount(BigDecimal.ZERO);  // 수정 시도
    assertThrows(IllegalStateException.class, () -> {
        ledgerRepository.save(entry);  // flush 시 예외
    });
}

@Test
void 원장_기록_삭제_시도시_예외_발생() {
    // Given: 원장 기록 생성
    LedgerEntry entry = ledgerRepository.save(createEntry());
    
    // When & Then: 삭제 시도 시 예외 발생
    assertThrows(IllegalStateException.class, () -> {
        ledgerRepository.delete(entry);
    });
}
```

### 2. 잔액 검증 테스트
```java
@Test
void 잔액_불일치_감지() {
    // Given: Account와 Ledger 잔액이 다른 상태
    
    // When: 잔액 검증 실행
    VerificationResult result = balanceVerificationService.verify(accountNumber);
    
    // Then:
    assertFalse(result.isMatch());
    verify(alertService).sendBalanceMismatchAlert(any());
}
```

### 3. API 테스트
```bash
# 원장 기록 조회
curl "http://localhost:8080/api/v1/ledger/entries?accountNumber=110-1234-5678-90" \
  -H "X-User-Id: USR-xxx" \
  -H "X-User-Role: USER"

# 특정 시점 잔액 조회
curl "http://localhost:8080/api/v1/ledger/balance?accountNumber=110-1234-5678-90&asOf=2024-01-15T00:00:00" \
  -H "X-User-Id: USR-xxx" \
  -H "X-User-Role: USER"
```

---

## 📝 구현 체크리스트

### Domain Layer ✅
- [x] LedgerErrorCode
- [x] LedgerException
- [x] EntryType (복식부기)
- [x] TransactionCategory
- [x] LedgerEntryId VO
- [x] AuditLogId VO
- [x] Money VO
- [x] LedgerEntry (Immutable)
- [x] AuditLog (Immutable)

### Application Layer
- [ ] LedgerEntryUseCase
- [ ] AuditLogUseCase
- [ ] BalanceVerificationService
- [ ] LedgerPort
- [ ] AuditLogPort
- [ ] DTO 정의
- [ ] BalanceVerificationScheduler

### Infrastructure Layer
- [ ] LedgerEntryEntity
- [ ] AuditLogEntity
- [ ] JpaRepository
- [ ] ImmutableEntity 마커 인터페이스
- [ ] AppendOnlyInterceptor (UPDATE/DELETE 차단)
- [ ] LedgerKafkaConsumer
- [ ] AccountServiceClient (Feign)
- [ ] DB 트리거 스크립트

### Presentation Layer
- [ ] LedgerController
- [ ] AuditLogController
- [ ] Request/Response DTO
- [ ] Swagger 문서화

### 테스트
- [ ] 도메인 단위 테스트
- [ ] Append-only 보호 테스트
- [ ] 잔액 검증 테스트
- [ ] 복식부기 검증 테스트