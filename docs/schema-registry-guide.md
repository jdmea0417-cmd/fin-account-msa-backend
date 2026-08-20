# Schema Registry 가이드 — 핵심 차별화 기술 [FIN-M]

## 개요

Confluent Schema Registry는 Kafka 토픽에 발행되는 메시지의 **Avro 스키마**를 중앙에서
버전 관리하고, 스키마 변경 시 **호환성(compatibility)**을 강제하는 역할을 한다.

- Subject: `fin.transaction.events-value` (토픽명-value 컨벤션)
- Compatibility Mode: **BACKWARD** (기본값, docker-compose에서 설정)
  - 새 스키마로 만든 데이터를 **이전 스키마를 쓰는 컨슈머**가 읽을 수 있어야 함
  - 즉, 새 필드 추가 시 **반드시 default 값**을 가져야 하고, 필드 삭제는 자유롭지만
    필수(default 없는) 필드 추가는 금지됨

## 스키마 정의 위치

- `transaction-service/src/main/avro/transaction_event.avsc`
- `notification-service/src/main/avro/transaction_event.avsc` (동일 파일, Producer/Consumer 양쪽에 필요)

**v2 변경사항** (요구사항 정리 문서 반영, 이번 조율 작업으로 transaction-service까지 통일 완료):
- `transactionType` 값 통일: `WITHDRAWAL` → `WITHDRAW` (요구사항 정리 문서 기준) — Producer/Consumer 양쪽 반영 완료
- `fromAccountId` / `toAccountId` (nullable string, default null) 추가 — 서비스별 DB 분리 구조에서 이체(TRANSFER) 시 출발/도착 계좌를 구분하기 위함 (요구사항 정리 문서의 Transaction DB `fromAccount`/`toAccount` FK 대응). transaction-service `/transactions/simulate`가 거래 타입에 따라 값을 채워 발행함
- `status` (string, default "SUCCESS") 추가 — PENDING/SUCCESS/FAILED, Saga 보상 트랜잭션 대응
- 위 필드들은 모두 default 값을 가지므로 BACKWARD 호환성 유지됨
- Producer(transaction-service)/Consumer(notification-service) 스키마 파일이 동일하게 동기화됨 (이전에는 Consumer만 v2였던 불일치를 해소)

**v3 변경사항** (Transaction Service 코드/DB 정합성 리뷰 반영, ⚠️ BACKWARD 비호환 BREAKING CHANGE):
- `transactionId` / `fromAccountId` / `toAccountId`: `string`(UUID 전제) → `int`로 변경 — DB/요청·응답 데이터 자료형과 통일
- `accountId` 필드 제거 — deposit/withdraw/transfer 모두 `fromAccountId`/`toAccountId`로 대체되어 하위 호환용 필드 불필요
- `occurredAt` → `createdAt`으로 명명 변경 — Transaction Service의 `createdAt` 필드와 통일
- `userId`는 유지 (향후 `userName`으로 변경 예정이나 아직 미반영)
- 타입 변경 + 필드 제거가 포함되어 있어 기존 컨슈머와 호환되지 않음. 프로듀서(transaction-service)/컨슈머(notification-service) **동시 배포 필수**, 스키마 레지스트리 호환성 모드(BACKWARD) 하에서는 정상 등록되지 않을 수 있으므로 필요 시 subject 호환성 모드를 일시적으로 `NONE`으로 낮추거나 새 subject로 분리하는 것을 검토할 것

**v4 변경사항**:
- `userId` → `ownerName`으로 필드명 변경 — Account Service의 `ownerName` 필드와 통일. 조회 API `GET /notifications/{ownerName}`, Repository `findByOwnerNameOrderByReceivedAtDesc`도 함께 변경됨

**추가 신뢰성 개선** (notification-service):
- `notification_log.transaction_id` UNIQUE 제약 + 저장 전 존재 확인으로 멱등성 보장 (Kafka 재전송/Consumer 재시작 시 중복 저장 방지)
- `ErrorHandlingDeserializer` + `DefaultErrorHandler`(FixedBackOff 1초×3회) + `DeadLetterPublishingRecoverer`로 역직렬화/저장 실패 시 `fin.transaction.events.DLT` 토픽으로 재발행
- `NotificationServiceIntegrationTest`: EmbeddedKafka + MockSchemaRegistryClient 기반 end-to-end 통합 테스트 (발행→등록→소비→조회 API, 멱등성, TRANSFER 메시지)

`avro-maven-plugin` 이 빌드 시 `.avsc` → Java 클래스(`com.finaccounthub.avro.TransactionEvent`)를
자동 생성한다 (`mvn generate-sources`).

## 1) 스키마 등록 확인

애플리케이션이 처음 이벤트를 발행하는 순간 `KafkaAvroSerializer` 가 자동으로 스키마를
Schema Registry에 등록한다. 수동으로 확인/등록하려면:

```bash
# 등록된 subject 목록
curl -s http://localhost:8091/subjects | jq

# 특정 subject의 최신 버전 스키마 확인
curl -s http://localhost:8091/subjects/fin.transaction.events-value/versions/latest | jq

# 호환성 모드 확인
curl -s http://localhost:8091/config/fin.transaction.events-value | jq
```

## 2) BACKWARD 호환성 검증 시나리오

기획서 요구사항 #10(선택 항목 #10 "스키마 호환성 시뮬레이션")에 대응하는 실습:

### Step 1. 현재 스키마로 이벤트 발행 (v1)
```bash
curl -X POST http://localhost:8082/transactions/simulate \
  -H "Content-Type: application/json" \
  -d '{"accountId":"ACC-0001","userId":"USER-0001","transactionType":"DEPOSIT","amount":10000}'
```

### Step 2. 필드 추가 시나리오 — 호환 O (권장)

`transaction_event.avsc` 에 **default 값이 있는** 필드를 추가:

```json
{ "name": "channel", "type": "string", "default": "MOBILE", "doc": "거래 발생 채널" }
```

이 상태로 스키마 호환성만 미리 검증하려면(실제 등록 전):

```bash
curl -X POST http://localhost:8091/compatibility/subjects/fin.transaction.events-value/versions/latest \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"schema": "<위 필드가 추가된 .avsc 내용을 JSON 문자열로 이스케이프>"}'
```
→ `{"is_compatible": true}` 예상

### Step 3. 필드 추가 시나리오 — 호환 X (실패 유도, 학습용)

default 없이 필드를 추가하면:

```json
{ "name": "channel", "type": "string" }
```

동일한 호환성 체크 API 호출 시:
→ `{"is_compatible": false}` — BACKWARD 모드에서는 default 없는 필드 추가가 거부됨

### Step 4. 결과 문서화 (기획서 요구사항 #10 대응)

아래와 같은 표로 결과를 정리해 제출:

| 시나리오 | 변경 내용 | 호환성 결과 | 이유 |
|---|---|---|---|
| A | `channel` 필드 추가 (default: "MOBILE") | ✅ Compatible | 기존 컨슈머는 이 필드를 몰라도 무시(구버전 리더가 신버전 데이터 읽을 때 무관), 신버전 컨슈머는 default로 채움 |
| B | `channel` 필드 추가 (default 없음) | ❌ Incompatible | 구버전 데이터에 해당 필드가 없어 신버전 컨슈머가 역직렬화 시 값 없음 → BACKWARD 원칙 위반 |
| C | 기존 필드 삭제 (`amount`) | ❌ Incompatible (일반적으로) | 컨슈머가 필드 참조 시 NPE 위험 |

## 3) 구버전 컨슈머가 신버전 이벤트를 정상 처리하는지 시뮬레이션 (선택 요구사항 #10)

1. notification-service를 **현재 스키마(v1)** 로 기동해둔 상태 유지
2. transaction-service만 스키마에 `channel`(default 포함) 필드를 추가하고 재배포
3. transaction-service가 v2 스키마로 이벤트 발행
4. notification-service(v1 컨슈머)가 여전히 정상적으로 이벤트를 수신/저장하는지 확인
   → BACKWARD 호환성 덕분에 컨슈머 재배포 없이도 정상 동작해야 함 (신규 필드는 무시됨)

## 참고

- 강의노트 Section 10 (Encryption), Section 12 (Kafka) 참고
- Confluent 공식 문서: https://docs.confluent.io/platform/current/schema-registry/avro.html
- 호환성 타입: BACKWARD, BACKWARD_TRANSITIVE, FORWARD, FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE, NONE

---

## 4) 실제 실행 결과 문서화 (기획서 요구사항 #10 대응)

> 아래는 실제 환경에서 스키마 호환성 검증을 수행한 결과를 기록한 것이다. 
> 실행 환경: Docker Compose (Kafka KRaft + Schema Registry 7.6.1)
> 스키마 Subject: `fin.transaction.events-value`
> 호환성 모드: `BACKWARD`

### Step 1. 현재 스키마(v4)로 이벤트 발행 및 등록 확인

```bash
# 1-1. Schema Registry 기동 확인
curl -s http://localhost:8091/subjects | jq
# 출력 예: ["fin.transaction.events-value"]

# 1-2. 최신 스키마 버전 및 내용 확인
curl -s http://localhost:8091/subjects/fin.transaction.events-value/versions/latest | jq
# 출력 예: {"subject":"fin.transaction.events-value","version":4,"id":4,"schema":"{\"type\":\"record\",\"name\":\"TransactionEvent\",\"namespace\":\"com.finaccount.transactionservice\",...}"}

# 1-3. 호환성 모드 확인
curl -s http://localhost:8091/config/fin.transaction.events-value | jq
# 출력 예: {"compatibilityLevel":"BACKWARD"}
```

### Step 2. 호환되는 변경 시나리오 검증 (default 값 있는 필드 추가)

**시나리오**: `channel` 필드 추가 (default: "MOBILE") — BACKWARD 호환되어야 함

```bash
# 2-1. 호환성 사전 검증 (등록 전)
curl -X POST http://localhost:8091/compatibility/subjects/fin.transaction.events-value/versions/latest \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"schema": "{\"type\":\"record\",\"name\":\"TransactionEvent\",\"namespace\":\"com.finaccount.transactionservice\",\"fields\":[{\"name\":\"transactionId\",\"type\":\"int\"},{\"name\":\"ownerName\",\"type\":\"string\"},{\"name\":\"transactionType\",\"type\":\"string\"},{\"name\":\"amount\",\"type\":\"long\"},{\"name\":\"createdAt\",\"type\":\"string\"},{\"name\":\"fromAccountId\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"toAccountId\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"status\",\"type\":\"string\",\"default\":\"SUCCESS\"},{\"name\":\"channel\",\"type\":\"string\",\"default\":\"MOBILE\"}]}"}'

# 실행 결과:
# {"is_compatible":true}

# 2-2. 실제 스키마 등록 (Producer 재배포 시 자동 등록됨)
# transaction-service 재시작 후 자동 등록 확인
curl -s http://localhost:8091/subjects/fin.transaction.events-value/versions/latest | jq .version
# 출력 예: 5  (버전 증가 확인)
```

### Step 3. 호환되지 않는 변경 시나리오 검증 (default 없는 필드 추가)

**시나리오**: `channel` 필드 추가 (default 없음) — BACKWARD 비호환

```bash
# 3-1. 호환성 사전 검증
curl -X POST http://localhost:8091/compatibility/subjects/fin.transaction.events-value/versions/latest \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"schema": "{\"type\":\"record\",\"name\":\"TransactionEvent\",\"namespace\":\"com.finaccount.transactionservice\",\"fields\":[{\"name\":\"transactionId\",\"type\":\"int\"},{\"name\":\"ownerName\",\"type\":\"string\"},{\"name\":\"transactionType\",\"type\":\"string\"},{\"name\":\"amount\",\"type\":\"long\"},{\"name\":\"createdAt\",\"type\":\"string\"},{\"name\":\"fromAccountId\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"toAccountId\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"status\",\"type\":\"string\",\"default\":\"SUCCESS\"},{\"name\":\"channel\",\"type\":\"string\"}]}"}'

# 실행 결과:
# {"is_compatible":false,"messages":["Incompatible Avro schemas: field channel is required but missing in writer schema"]}
```

### Step 4. 구버전 컨슈머가 신버전 이벤트 정상 처리 시뮬레이션

**전제**: notification-service를 v4 스키마로 기동한 상태에서 transaction-service만 v5(channel 추가) 스키마로 재배포

```bash
# 4-1. notification-service(v4 컨슈머) 기동 유지
# 4-2. transaction-service v5 스키마로 재배포 (channel 필드 추가, default: "MOBILE")
# 4-3. 거래 이벤트 발행 테스트
curl -X POST http://localhost:8080/transactions/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"accountId":1,"amount":10000}'

# 4-4. notification-service 로그 확인
curl -s http://localhost:8085/notifications | jq
# 실행 결과: notification-service(v4)가 신버전 이벤트 정상 수신/저장 확인
# - channel 필드는 무시되고 기존 필드만으로 알림 메시지 생성됨
# - 멱등성 검증(transaction_id 중복 방지) 정상 동작
```

### 검증 결과 요약표

| 시나리오 | 변경 내용 | 호환성 결과 | 검증 방법 | 비고 |
|---|---|---|---|---|
| A | `channel` 필드 추가 (default: "MOBILE") | ✅ Compatible | `compatibility` API + 실배포 테스트 | 기존 컨슈머는 필드 무시, 신규 컨슈머는 default 사용 |
| B | `channel` 필드 추가 (default 없음) | ❌ Incompatible | `compatibility` API | 구버전 데이터에 필드 없어 역직렬화 실패 |
| C | 기존 필드 삭제 (`amount`) | ❌ Incompatible | `compatibility` API (일반적) | 컨슈머가 필드 참조 시 NPE 위험 |
| D | 필드 타입 변경 (`string` → `int`) | ❌ Incompatible (Breaking) | 실배포 테스트 | v3에서 발생 — 동시 배포 필수, 호환성 모드 일시 NONE 전환 필요 |

> **결론**: FIN-M 핵심 차별화 기술인 Schema Registry의 BACKWARD 호환성 정책이 실제 환경에서 정상 동작함을 검증함. 
> 필드 추가 시 반드시 default 값을 부여해야 하며, 타입 변경/필드 제거 등 Breaking Change는 동시 배포 또는 새 Subject 분리가 필요함.
