# Schema Registry 호환성 시뮬레이션 및 검증 리포트 [FIN-M]

## 1. 개요 및 목적

Confluent Schema Registry를 활용하여 `fin.transaction.events` 토픽에 발행되는 Avro 이벤트의 스키마 버전 관리 및 **BACKWARD 호환성 규칙** 강제 동작을 실제 API 호출과 시뮬레이션을 통해 검증한 결과를 기록한다.

- **대상 토픽**: `fin.transaction.events`
- **Subject 명**: `fin.transaction.events-value`
- **호환성 모드**: `BACKWARD` (기본값)
- **검증 도구**: `scripts/schema-registry-demo.sh`, curl, jq, Python urllib

---

## 2. 검증 시나리오 및 실행 결과

### 1) 현재 등록된 스키마 및 호환성 모드 확인

```bash
# Subject 목록 확인
curl -s http://localhost:8091/subjects
# 응답: ["fin.transaction.events-value"]

# 호환성 모드 조회
curl -s http://localhost:8091/config/fin.transaction.events-value
# 응답: {"compatibilityLevel":"BACKWARD"}

# 최신 스키마 버전 확인
curl -s http://localhost:8091/subjects/fin.transaction.events-value/versions/latest | jq
```

```json
{
  "subject": "fin.transaction.events-value",
  "version": 1,
  "id": 1,
  "schema": "{\"type\":\"record\",\"name\":\"TransactionEvent\",\"namespace\":\"com.finaccount.transactionservice\",\"fields\":[{\"name\":\"transactionId\",\"type\":\"int\"},{\"name\":\"ownerName\",\"type\":\"string\"},{\"name\":\"transactionType\",\"type\":\"string\"},{\"name\":\"amount\",\"type\":\"long\"},{\"name\":\"createdAt\",\"type\":\"string\"},{\"name\":\"fromAccountId\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"toAccountId\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"status\",\"type\":\"string\",\"default\":\"SUCCESS\"}]}"
}
```

---

### 2) 시나리오 A: Default 값이 있는 신규 필드 추가 (호환성 O)

- **변경 내용**: 거래 채널 정보를 담는 `channel` 필드를 `default: "MOBILE"`과 함께 추가
- **검증 요청**:
  ```bash
  curl -X POST http://localhost:8091/compatibility/subjects/fin.transaction.events-value/versions/latest \
    -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    -d '{"schema":"{\"type\":\"record\",\"name\":\"TransactionEvent\",\"namespace\":\"com.finaccount.transactionservice\",\"fields\":[{\"name\":\"transactionId\",\"type\":\"int\"},{\"name\":\"ownerName\",\"type\":\"string\"},{\"name\":\"transactionType\",\"type\":\"string\"},{\"name\":\"amount\",\"type\":\"long\"},{\"name\":\"createdAt\",\"type\":\"string\"},{\"name\":\"fromAccountId\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"toAccountId\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"status\",\"type\":\"string\",\"default\":\"SUCCESS\"},{\"name\":\"channel\",\"type\":\"string\",\"default\":\"MOBILE\"}]}"}'
  ```
- **검증 결과**:
  ```json
  {
    "is_compatible": true
  }
  ```
- **원인 분석**: `BACKWARD` 모드에서는 구버전 리더(Consumer)가 신버전 데이터(Producer)를 읽을 때 신규 필드를 무시할 수 있으며, 신버전 리더가 구버전 데이터를 읽을 때 default 값으로 채우므로 호환성이 완벽하게 유지됨.

---

### 3) 시나리오 B: Default 값이 없는 신규 필드 추가 (호환성 X — 거부 유도)

- **변경 내용**: `channel` 필드를 default 없이 필수 필드로 추가
- **검증 요청**:
  ```bash
  curl -X POST http://localhost:8091/compatibility/subjects/fin.transaction.events-value/versions/latest \
    -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    -d '{"schema":"{\"type\":\"record\",\"name\":\"TransactionEvent\",\"namespace\":\"com.finaccount.transactionservice\",\"fields\":[{\"name\":\"transactionId\",\"type\":\"int\"},{\"name\":\"ownerName\",\"type\":\"string\"},{\"name\":\"transactionType\",\"type\":\"string\"},{\"name\":\"amount\",\"type\":\"long\"},{\"name\":\"createdAt\",\"type\":\"string\"},{\"name\":\"fromAccountId\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"toAccountId\",\"type\":[\"null\",\"int\"],\"default\":null},{\"name\":\"status\",\"type\":\"string\",\"default\":\"SUCCESS\"},{\"name\":\"channel\",\"type\":\"string\"}]}"}'
  ```
- **검증 결과**:
  ```json
  {
    "is_compatible": false
  }
  ```
- **원인 분석**: 신버전 컨슈머가 구버전 데이터를 읽으려 할 때 `channel` 필드의 기본값이 없어 역직렬화 에러가 발생하므로 Schema Registry가 등록을 사전에 차단함.

---

### 4) 시나리오 C: 구버전 컨슈머(v1)와 신버전 프로듀서(v2) 실동작 시뮬레이션

| 단계 | 수행 작업 | 결과 | 비고 |
|---|---|:---:|---|
| 1 | `notification-service`를 v1 스키마 상태로 기동 | 정상 기동 | `TransactionEventListener`가 v1 토픽 리스닝 |
| 2 | `transaction-service`에서 v2(`channel: "MOBILE"` 추가) 스키마로 이벤트 발행 | 발행 성공 | Schema Registry v2 자동 등록 |
| 3 | `notification-service`의 이벤트 수신 및 DB 저장 확인 | **정상 수신** | 신규 `channel` 필드는 안전하게 무시되고 알림 로그 생성 완료 |
| 4 | 멱등성 검증 (동일 `transactionId` 재전송) | **중복 무시** | UNIQUE 제약 및 사전 조회로 중복 저장 차단 |

---

## 3. 종합 검증 요약표

| 시나리오 | 스키마 변경 항목 | 호환성 결과 | Schema Registry 판정 | 서비스 동작 여부 |
|---|---|:---:|:---:|:---:|
| **시나리오 A** | `channel` (string, default: "MOBILE") 추가 | ✅ Compatible | `is_compatible: true` | 무중단 배포 및 정상 소비 |
| **시나리오 B** | `channel` (string, no default) 추가 | ❌ Incompatible | `is_compatible: false` | 등록 거부 (스키마 파괴 방지) |
| **시나리오 C** | 구버전 컨슈머 vs 신버전 프로듀서 연동 | ✅ Compatible | - | 컨슈머 재배포 없이 데이터 정상 수신 |
