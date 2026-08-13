# Fin-Account Hub — 디지털 계좌 통합 관리 플랫폼

- 프로젝트 코드: **[FIN-M]**
- 도메인: 금융 (중) / 핵심 차별화 기술: **Schema Registry**
- 참조 문서: `[CNS 5기] 미니PJT 2 기획서.pdf` (10장)

## 이번 스프린트 구현 범위

이 리포지토리는 **전체 MSA 골격(틀)** 을 갖추고 있으나, 실제 비즈니스 로직까지 완성된 서비스는
아래 3가지뿐입니다. 나머지는 Eureka 등록 + 헬스체크만 되는 **스켈레톤**이며 팀원이 이어서 구현합니다.

| 서비스 | 상태 | 설명 |
|---|---|---|
| **notification-service** | ✅ 완성 | Kafka(Avro) 컨슈머, 알림 로그 저장/조회 API |
| **Kafka 인프라 + 이벤트 파이프라인** | ✅ 완성 | Kafka(KRaft, 단일 노드) + transaction-service의 Producer(데모용) → notification-service의 Consumer |
| **Schema Registry** | ✅ 완성 | Confluent Schema Registry, Avro 스키마 등록/조회, BACKWARD 호환성 검증 스크립트 |
| discovery-service (Eureka) | 🚧 스켈레톤 | 기본 구동만 확인됨 |
| config-service (Config Server) | 🚧 스켈레톤 | native 파일 기반, 실제 Git 저장소 전환은 TODO |
| apigateway-service (Gateway) | 🚧 스켈레톤 | 라우팅 설정만 있음, JWT 인증 필터는 TODO |
| account-service (계좌관리) | 🚧 스켈레톤 | health-check만 존재, 계좌 Entity/CRUD는 TODO |
| transaction-service (입출금/이체) | 🚧 스켈레톤 + Kafka Producer만 구현 | 실제 이체 로직은 TODO, `/transactions/simulate`로 이벤트 발행 데모만 가능 |

각 스켈레톤 서비스 폴더에는 `README-TODO.md` 로 남은 작업을 정리해뒀습니다.

## 기술 스택

- Java **21**, Spring Boot 3.3.4, Spring Cloud 2023.0.3
- Kafka (Apache Kafka 3.8, KRaft 모드, Zookeeper 불필요)
- Confluent Schema Registry 7.6.1 + Avro
- H2 (서비스별 임베디드 DB, 서비스당 DB 분리 원칙 적용)
- Docker / Docker Compose

## 폴더 구조

```
fin-account-msa-backend/
├── docker-compose.yml            # 전체 스택 (Kafka, Schema Registry, 6개 서비스)
├── docs/
│   ├── architecture.md           # 시스템 구성도 (텍스트 다이어그램)
│   └── schema-registry-guide.md  # Avro 스키마 등록/호환성 검증 가이드
├── scripts/
│   └── schema-registry-demo.sh   # 스키마 등록 + BACKWARD 호환성 테스트 curl 스크립트
├── discovery-service/             # Eureka Server
├── config-service/                # Spring Cloud Config Server (native backend)
├── apigateway-service/            # Spring Cloud Gateway
├── account-service/                # 계좌관리 MS (스켈레톤)
├── transaction-service/            # 입출금/이체 MS (스켈레톤 + Kafka Producer)
└── notification-service/           # 알림 MS (완성)
```

## 실행 방법

### 1) 전체 스택 Docker Compose로 기동

```bash
cd fin-account-msa-backend
docker compose build
docker compose up -d
```

기동 순서: `kafka` → `schema-registry` → `discovery-service` → `config-service` → 나머지 서비스 (docker-compose의 `depends_on` + healthcheck로 제어)

### 2) 개별 서비스 로컬 실행 (IDE)

각 서비스 폴더에서 JDK 21 기준으로 `./mvnw spring-boot:run` (또는 IDE Run) 로 개별 기동 가능.
단, Kafka/Schema Registry/Config/Eureka 는 미리 기동되어 있어야 합니다.

### 3) Kafka 이벤트 데모 (transaction-service → notification-service)

```bash
curl -X POST http://localhost:8082/transactions/simulate \
  -H "Content-Type: application/json" \
  -d '{"accountId":"ACC-0001","userId":"USER-0001","transactionType":"DEPOSIT","amount":50000}'
```

위 요청이 `transaction-service` 에서 Avro로 직렬화되어 `fin.transaction.events` 토픽에 발행되고,
`notification-service` 가 이를 구독해 알림 로그로 저장합니다. 확인:

```bash
curl http://localhost:8085/notifications
```

### 4) Schema Registry 확인

```bash
curl http://localhost:8081/subjects
curl http://localhost:8081/subjects/fin.transaction.events-value/versions/latest
```

호환성 검증 데모는 `scripts/schema-registry-demo.sh` 및 `docs/schema-registry-guide.md` 참고.

## 서비스 & 포트

| 서비스 | 포트 | 비고 |
|---|---|---|
| discovery-service (Eureka) | 8761 | http://localhost:8761 |
| config-service | 8888 | http://localhost:8888 |
| apigateway-service | 8000 | 단일 진입점 |
| account-service | 8081 | 랜덤 포트 대신 고정(로컬 편의) |
| transaction-service | 8082 | |
| notification-service | 8085 | |
| Kafka broker | 9092 (내부), 29092 (호스트) | KRaft 단일 노드 |
| Schema Registry | 8081(REST)→**8091**로 매핑 (account-service와 포트 충돌 방지) | 컨테이너 내부는 8081 |
| Kafka UI (선택) | 8090 | http://localhost:8090 |

> ⚠️ account-service 컨테이너 포트(8081)와 schema-registry 컨테이너 기본 포트(8081)가 겹쳐서
> docker-compose에서는 schema-registry 호스트 포트를 8091로 매핑했습니다. (컨테이너 간 통신은 문제 없음, 컨테이너명:8081로 통신)

## 다음 단계 (팀원 TODO)

1. **account-service**: Account 엔티티/Repository/Service/Controller 구현 (계좌 개설/조회/입출금)
2. **transaction-service**: 실제 이체 로직 + OpenFeign으로 account-service 잔액 조회/차감 (Saga 단순 보상 트랜잭션)
3. **apigateway-service**: JWT 인증 필터(`AuthorizationHeaderFilter`) 구현
4. **config-service**: `config-repo`를 실제 Git 저장소로 전환 (Section 8 강의노트 참고)
5. 선택 요구사항 3개 이상 선정 (Spring Cloud Bus, Circuit Breaker, 모니터링 등)
