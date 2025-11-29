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
│   │   1   │    A    │ 50,000  │    0    │ 이체 출금    │   │
│   │   1   │    B    │    0    │ 50,000  │ 이체 입금    │   │
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

### LedgerEntry Entity (원장 기록)

```
┌─────────────────────────────────────────────┐
│               LedgerEntry                    │
├─────────────────────────────────────────────┤
│ id: Long (PK, Auto)                         │
│ entryId: String (UUID, Unique)              │
│ transactionId: String (원본 거래 ID)         │
│ accountNumber: String                       │
│ entryType: EntryType (DEBIT/CREDIT)         │
│ amount: BigDecimal                          │
│ balanceAfter: BigDecimal (거래 후 잔액)      │
│ description: String                         │
│ category: TransactionCategory               │
│ referenceType: String (원본 서비스)          │
│ referenceId: String (원본 ID)               │
│ createdAt: LocalDateTime (불변)             │
│                                             │
│ ⚠️ 이 테이블은 INSERT만 허용!               │
│ ⚠️ UPDATE/DELETE 금지!                      │
└─────────────────────────────────────────────┘
```

### AuditLog Entity (감사 로그)

```
┌─────────────────────────────────────────────┐
│                AuditLog                      │
├─────────────────────────────────────────────┤
│ id: Long (PK, Auto)                         │
│ eventId: String (UUID)                      │
│ eventType: String                           │
│ serviceName: String (발생 서비스)            │
│ userId: Long                                │
│ resourceType: String (ex: "Account")        │
│ resourceId: String                          │
│ action: String (ex: "BALANCE_CHANGED")      │
│ previousValue: String (JSON)                │
│ newValue: String (JSON)                     │
│ ipAddress: String                           │
│ userAgent: String                           │
│ timestamp: LocalDateTime                    │
│                                             │
│ ⚠️ 이 테이블도 INSERT만 허용!               │
└─────────────────────────────────────────────┘
```

### EntryType Enum
```java
public enum EntryType {
    DEBIT,   // 차변 (자산 증가, 부채 감소)
    CREDIT   // 대변 (자산 감소, 부채 증가)
}
```

### TransactionCategory Enum
```java
public enum TransactionCategory {
    DEPOSIT,       // 입금
    WITHDRAWAL,    // 출금
    TRANSFER_IN,   // 이체 입금
    TRANSFER_OUT,  // 이체 출금
    PAYMENT,       // 결제
    REFUND,        // 환불
    FEE,           // 수수료
    INTEREST       // 이자
}
```

---

## 📡 API 명세

### 1. 원장 기록 조회 (계좌별)
```http
GET /api/v1/ledger/entries?accountNumber=110-1234-5678-90&page=0&size=20
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "accountNumber": "110-1234-5678-90",
  "entries": [
    {
      "entryId": "entry-uuid-1",
      "transactionId": "txn-uuid-abcd",
      "entryType": "CREDIT",
      "amount": 100000,
      "balanceAfter": 250000,
      "description": "급여 입금",
      "category": "DEPOSIT",
      "createdAt": "2024-01-15T10:30:00"
    },
    {
      "entryId": "entry-uuid-2",
      "transactionId": "txn-uuid-efgh",
      "entryType": "DEBIT",
      "amount": 50000,
      "balanceAfter": 200000,
      "description": "ATM 출금",
      "category": "WITHDRAWAL",
      "createdAt": "2024-01-15T11:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150
}
```

---

### 2. 특정 시점 잔액 조회
```http
GET /api/v1/ledger/balance?accountNumber=110-1234-5678-90&asOf=2024-01-15T00:00:00
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "accountNumber": "110-1234-5678-90",
  "asOf": "2024-01-15T00:00:00",
  "balance": 150000,
  "calculatedFrom": "ledger_entries"
}
```

---

### 3. 기간별 거래 요약
```http
GET /api/v1/ledger/summary?accountNumber=110-1234-5678-90&startDate=2024-01-01&endDate=2024-01-31
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "accountNumber": "110-1234-5678-90",
  "period": {
    "start": "2024-01-01",
    "end": "2024-01-31"
  },
  "openingBalance": 100000,
  "closingBalance": 250000,
  "summary": {
    "totalDebit": 80000,
    "totalCredit": 230000,
    "netChange": 150000
  },
  "byCategory": {
    "DEPOSIT": 200000,
    "WITHDRAWAL": -50000,
    "TRANSFER_IN": 30000,
    "TRANSFER_OUT": -10000,
    "PAYMENT": -20000
  },
  "transactionCount": 25
}
```

---

### 4. 잔액 검증 (관리자)
```http
POST /api/v1/ledger/verify
X-User-Id: 999
X-User-Role: ADMIN
Content-Type: application/json

{
  "accountNumbers": ["110-1234-5678-90", "110-9876-5432-10"]
}
```

**Response (200 OK)**
```json
{
  "verifiedAt": "2024-01-15T02:00:00",
  "results": [
    {
      "accountNumber": "110-1234-5678-90",
      "accountBalance": 250000,
      "ledgerBalance": 250000,
      "match": true
    },
    {
      "accountNumber": "110-9876-5432-10",
      "accountBalance": 500000,
      "ledgerBalance": 499000,
      "match": false,
      "difference": 1000,
      "alertSent": true
    }
  ],
  "totalVerified": 2,
  "mismatches": 1
}
```

**이벤트 발행**: `ledger.balance.mismatch` (불일치 시)

---

### 5. 감사 로그 조회 (관리자)
```http
GET /api/v1/ledger/audit-logs?resourceType=Account&resourceId=1&page=0&size=20
X-User-Id: 999
X-User-Role: ADMIN
```

**Response (200 OK)**
```json
{
  "logs": [
    {
      "eventId": "audit-uuid-1",
      "eventType": "BALANCE_CHANGED",
      "serviceName": "account-service",
      "userId": 1,
      "resourceType": "Account",
      "resourceId": "1",
      "action": "DEPOSIT",
      "previousValue": {"balance": 150000},
      "newValue": {"balance": 250000},
      "ipAddress": "192.168.1.1",
      "timestamp": "2024-01-15T10:30:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 500
}
```

---

### 6. 계좌 명세서 생성
```http
POST /api/v1/ledger/statements
X-User-Id: 1
X-User-Role: USER
Content-Type: application/json

{
  "accountNumber": "110-1234-5678-90",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "format": "PDF"
}
```

**Response (202 Accepted)**
```json
{
  "statementId": "stmt-uuid-1234",
  "status": "GENERATING",
  "message": "명세서 생성 중입니다. 완료 시 알림을 드립니다."
}
```

---

## 📂 패키지 구조

```
com.junbank.ledger
├── LedgerServiceApplication.java
├── domain
│   ├── entity
│   │   ├── LedgerEntry.java
│   │   └── AuditLog.java
│   ├── enums
│   │   ├── EntryType.java
│   │   └── TransactionCategory.java
│   └── repository
│       ├── LedgerEntryRepository.java
│       └── AuditLogRepository.java
├── application
│   ├── service
│   │   ├── LedgerService.java
│   │   ├── AuditLogService.java
│   │   └── BalanceVerificationService.java
│   ├── dto
│   │   ├── request
│   │   │   └── VerifyBalanceRequest.java
│   │   └── response
│   │       ├── LedgerEntryResponse.java
│   │       ├── BalanceSummaryResponse.java
│   │       └── VerificationResultResponse.java
│   └── scheduler
│       └── BalanceVerificationScheduler.java
├── infrastructure
│   ├── kafka
│   │   └── LedgerEventConsumer.java  (주로 수신만)
│   ├── feign
│   │   └── AccountServiceClient.java
│   ├── protection
│   │   ├── AppendOnlyInterceptor.java
│   │   └── ImmutableEntity.java
│   └── config
│       ├── JpaConfig.java
│       └── KafkaConfig.java
└── presentation
    ├── controller
    │   └── LedgerController.java
    └── advice
        └── LedgerExceptionHandler.java
```

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
  -H "X-User-Id: 1" \
  -H "X-User-Role: USER"

# 특정 시점 잔액 조회
curl "http://localhost:8080/api/v1/ledger/balance?accountNumber=110-1234-5678-90&asOf=2024-01-15T00:00:00" \
  -H "X-User-Id: 1" \
  -H "X-User-Role: USER"
```

---

## 📝 구현 체크리스트

- [ ] Entity, Repository 생성
- [ ] **ImmutableEntity 마커 인터페이스**
- [ ] **AppendOnlyInterceptor 구현**
- [ ] LedgerService 구현
- [ ] AuditLogService 구현
- [ ] **BalanceVerificationService 구현**
- [ ] **BalanceVerificationScheduler 구현**
- [ ] Controller 구현
- [ ] **Kafka Consumer 구현 (다양한 이벤트 수신)**
- [ ] Feign Client 구현 (Account Service)
- [ ] **DB 트리거 스크립트 작성**
- [ ] Append-only 테스트 코드
- [ ] 잔액 검증 테스트 코드
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] API 문서화 (Swagger)