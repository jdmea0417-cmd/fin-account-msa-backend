# Fin-Account Hub Kubernetes Manifests

이 디렉토리는 Fin-Account Hub MSA 프로젝트의 Kubernetes 배포 매니페스트를 포함합니다.

## 디렉토리 구조

```
k8s/
├── base/                    # 기본 매니페스트 (모든 환경 공통)
│   ├── 00-namespace.yaml
│   ├── 01-configmap.yaml
│   ├── 02-secrets.yaml
│   ├── 03-mariadb-account-statefulset.yaml
│   ├── 04-mariadb-account-service.yaml
│   ├── 05-mariadb-transaction-statefulset.yaml
│   ├── 06-mariadb-transaction-service.yaml
│   ├── 07-mariadb-notification-statefulset.yaml
│   ├── 08-mariadb-notification-service.yaml
│   ├── 09-kafka-deployment.yaml
│   ├── 10-kafka-pvc.yaml
│   ├── 11-kafka-service.yaml
│   ├── 12-schema-registry-deployment.yaml
│   ├── 13-schema-registry-service.yaml
│   ├── 14-kafka-ui-deployment.yaml
│   ├── 15-kafka-ui-service.yaml
│   ├── 16-config-service-deployment.yaml
│   ├── 17-config-service-service.yaml
│   ├── 18-eureka-service-deployment.yaml
│   ├── 19-eureka-service-service.yaml
│   ├── 20-gateway-deployment.yaml
│   ├── 21-gateway-service.yaml
│   ├── 22-account-service-deployment.yaml
│   ├── 23-account-service-service.yaml
│   ├── 24-transaction-service-deployment.yaml
│   ├── 25-transaction-service-service.yaml
│   ├── 26-notification-service-deployment.yaml
│   ├── 27-notification-service-service.yaml
│   ├── 28-ingress.yaml
│   └── kustomization.yaml
└── overlays/
    └── dev/                 # 개발 환경 오버레이
        └── kustomization.yaml
```

## 배포 방법

### 사전 요구사항
- Kubernetes 클러스터 (minikube, k3s, kind, 또는 클라우드 매니지드 K8s)
- `kubectl` 설치 및 클러스터 연결
- `kustomize` 설치 (또는 `kubectl apply -k` 사용)
- NGINX Ingress Controller 설치 (Ingress 사용 시)

### 1. 개발 환경 배포 (minikube/k3s 권장)

```bash
# minikube 예시
minikube start --cpus=4 --memory=8192 --disk-size=20g
minikube addons enable ingress

# k3s 예시
# k3s는 기본적으로 traefik ingress 포함

# 네임스페이스 및 리소스 생성
kubectl apply -k k8s/overlays/dev

# 또는 kustomize build 후 적용
kustomize build k8s/overlays/dev | kubectl apply -f -
```

### 2. 배포 상태 확인

```bash
# 모든 리소스 상태 확인
kubectl get all -n fin-account-hub

# 파드 상태 확인
kubectl get pods -n fin-account-hub -w

# 특정 파드 로그 확인
kubectl logs -n fin-account-hub -l app=gateway -f

# 서비스 엔드포인트 확인
kubectl get svc -n fin-account-hub
```

### 3. 접속 확인

```bash
# Ingress 호스트 추가 (minikube의 경우)
echo "$(minikube ip) fin-account.local" | sudo tee -a /etc/hosts

# API 테스트
curl http://fin-account.local/actuator/health
curl http://fin-account.local/accounts/1 -H "Authorization: Bearer <JWT_TOKEN>"

# Kafka UI 접속
kubectl port-forward -n fin-account-hub svc/kafka-ui 8090:8080
# 브라우저에서 http://localhost:8090 접속

# Eureka Dashboard 접속
kubectl port-forward -n fin-account-hub svc/eureka-service 8761:8761
# 브라우저에서 http://localhost:8761 접속
```

### 4. 정리

```bash
# 전체 리소스 삭제
kubectl delete -k k8s/overlays/dev

# 네임스페이스만 삭제 (모든 리소스 포함)
kubectl delete namespace fin-account-hub
```

## 주요 구성 요소

| 컴포넌트 | 타입 | 복제본(개발) | 포트 | 설명 |
|---|---|---|---|---|
| Kafka | Deployment | 1 | 9092, 29092 | KRaft 모드 메시지 브로커 |
| Schema Registry | Deployment | 1 | 8081 | Avro 스키마 관리 (BACKWARD 호환성) |
| Kafka UI | Deployment | 1 | 8080 | Kafka 토픽/스키마 모니터링 |
| MariaDB (Account) | StatefulSet | 1 | 3306 | 계좌 서비스 DB |
| MariaDB (Transaction) | StatefulSet | 1 | 3306 | 거래 서비스 DB |
| MariaDB (Notification) | StatefulSet | 1 | 3306 | 알림 서비스 DB |
| Config Service | Deployment | 1 | 8888 | Spring Cloud Config Server |
| Eureka Service | Deployment | 1 | 8761 | Service Discovery |
| Gateway | Deployment | 1 | 8080 | API Gateway (JWT 인증, 라우팅) |
| Account Service | Deployment | 1 | 8080 | 계좌 관리 (REST, JWT 발급) |
| Transaction Service | Deployment | 1 | 8080 | 입출금/이체, Kafka Producer |
| Notification Service | Deployment | 1 | 8085 | Kafka Consumer, 알림 로그 |

## 환경 변수 관리

- `k8s/base/01-configmap.yaml`: 공통 설정 (Kafka, Eureka, Schema Registry URL 등)
- `k8s/base/02-secrets.yaml`: 민감 정보 (DB 비밀번호 등)
- 각 Deployment는 `envFrom`으로 ConfigMap과 Secret 참조

## 주의사항

1. **이미지**: `uniquecolor/*` 이미지는 Docker Hub에 사전 빌드되어 있어야 함
   - CI/CD 파이프라인에서 자동 빌드/푸시 권장
   - 로컬 테스트 시 `docker build -t uniquecolor/서비스명:1.0.0 ./서비스명` 후 `minikube image load` 필요

2. **리소스 요청량**: 개발 환경 기준 (CPU 250m~500m, Memory 256Mi~512Mi)
   - 프로덕션 환경에서는 `overlays/prod` 생성 후 리소스 증설 필요

3. **Persistence**: Kafka와 MariaDB는 PVC 사용
   - minikube/k3s 기본 StorageClass 사용
   - 클라우드 환경에서는 적절한 StorageClass 지정 필요

4. **Ingress**: `fin-account.local` 호스트 사용
   - 실제 도메인 사용 시 Ingress 호스트 및 TLS 설정 수정 필요

5. **Service Discovery**: Kubernetes DNS 기반 (`서비스명.네임스페이스.svc.cluster.local`)
   - Spring Cloud Kubernetes Discovery 연동 시 별도 설정 필요
   - 현재는 Eureka 사용하므로 Eureka Service 통해 등록/검색