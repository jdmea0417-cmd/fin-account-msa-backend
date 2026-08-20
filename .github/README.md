# Fin-Account Hub CI/CD Pipeline

이 문서는 GitHub Actions 기반 CI/CD 파이프라인의 구성과 사용 방법을 설명합니다.

## 파이프라인 구조

```
.github/workflows/
├── ci.yml              # 메인 CI/CD 파이프라인
└── dependabot.yml      # 의존성 자동 업데이트
```

## 워크플로우 단계

### 1. Build & Test (`build-and-test`)
- **트리거**: push, PR (main, develop 브랜치)
- **동작**:
  - Maven 의존성 캐싱
  - 전체 모듈 컴파일 (`mvn clean compile`)
  - 단위 테스트 실행 (`mvn test`)
  - 테스트 리포트 생성 (JUnit XML → GitHub Actions UI)
  - JaCoCo 커버리지 아티팩트 업로드

### 2. Docker Build (`docker-build`)
- **트리거**: `build-and-test` 성공 후
- **전략**: 6개 서비스 병렬 빌드 (matrix strategy)
- **동작**:
  - Docker Buildx로 멀티플랫폼 빌드 (linux/amd64)
  - Docker Hub 푸시 (`uniquecolor/서비스명`)
  - 태그 전략: `latest`, `v1.0.0`, `v1.0`, `sha-<commit>`, `branch-<name>`
  - GitHub Actions 캐시 레이어 활용

### 3. Integration Test (`integration-test`)
- **트리거**: push, PR
- **동작**:
  - Docker Compose로 전체 스택 기동 (Kafka, Schema Registry, MariaDB×3, 6개 마이크로서비스)
  - 헬스체크 대기 (최대 10분)
  - API 스모크 테스트:
    - 계좌 생성 → 로그인(JWT) → 입금 → 출금 → 알림 조회
  - 실패 시 전체 로그 수집
  - 종료 시 `docker compose down -v`로 볼륨 정리

### 4. Security Scan (`security-scan`)
- **트리거**: `docker-build` 성공 후
- **도구**: Trivy
- **동작**:
  - 각 서비스 이미지별 취약점 스캔 (CRITICAL, HIGH)
  - SARIF 형식으로 GitHub Security 탭 업로드

### 5. K8s Validate (`k8s-validate`)
- **트리거**: `build-and-test` 성공 후
- **동작**:
  - `kustomize build`로 매니페스트 렌더링 검증
  - `kubeconform`으로 Kubernetes 스키마 검증 (v1.28.0)

### 6. Deploy Staging (`deploy-staging`)
- **트리거**: 태그 푸시 (`v*` 패턴)
- **환경**: `staging`
- **동작**:
  - 이미지 태그를 릴리스 버전으로 업데이트
  - `k8s/overlays/dev` 매니페스트 적용
  - 롤아웃 상태 대기 (최대 5분)
  - 헬스체크 검증

### 7. Deploy Production (`deploy-production`)
- **트리거**: 태그 푸시 + 수동 승인
- **환경**: `production`
- **동작**:
  - 수동 승인 필요 (GitHub Environments 보호 규칙)
  - Blue-Green 또는 Rolling 배포 전략 선택 가능

### 8. Notify (`notify`)
- **트리거**: 항상 (모든 작업 완료 후)
- **동작**:
  - Slack 웹훅으로 결과 알림 (성공/실패)
  - 파이프라인 요약 출력

## 필수 시크릿 설정

GitHub Repository Settings → Secrets and variables → Actions에서 다음 시크릿을 설정해야 합니다:

| 시크릿명 | 설명 | 예시 |
|---|---|---|
| `DOCKERHUB_USERNAME` | Docker Hub 사용자명 | `uniquecolor` |
| `DOCKERHUB_TOKEN` | Docker Hub 액세스 토큰 | `dckr_pat_xxx` |
| `STAGING_KUBECONFIG` | 스테이징 클러스터 kubeconfig (base64 인코딩) | `apiVersion: v1...` |
| `PRODUCTION_KUBECONFIG` | 프로덕션 클러스터 kubeconfig (base64 인코딩) | `apiVersion: v1...` |
| `SLACK_WEBHOOK_URL` | Slack Incoming Webhook URL (선택) | `https://hooks.slack.com/...` |

### kubeconfig 생성 방법
```bash
# 현재 컨텍스트의 kubeconfig를 base64로 인코딩
kubectl config view --flatten --minify | base64 -w 0
```

## 브랜치 전략

| 브랜치 | 용도 | 배포 대상 |
|---|---|---|
| `main` | 프로덕션 준비 코드 | 태그 푸시 시 Staging → Production |
| `develop` | 개발 통합 브랜치 | 매 커밋마다 통합 테스트 |
| `feature/*` | 기능 개발 | PR 시 빌드/테스트만 |
| `release/*` | 릴리스 준비 | Staging 배포 검증 |
| `hotfix/*` | 긴급 수정 | main에서 분기, 태그 후 배포 |

## 태그 및 릴리스

```bash
# 릴리스 태그 생성 (시맨틱 버저닝)
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# 태그 푸시 시 자동으로:
# 1. Docker 이미지에 v1.0.0, v1.0, latest 태그 추가
# 2. Staging 환경 배포
# 3. 수동 승인 후 Production 배포 가능
```

## 로컬에서 파이프라인 시뮬레이션

```bash
# act로 로컬 실행 (선택사항)
brew install act
act push --secret DOCKERHUB_USERNAME=xxx --secret DOCKERHUB_TOKEN=xxx

# 또는 각 단계 수동 실행
mvn clean test
docker compose -f docker-compose.yml up --build -d
# 스모크 테스트 수행
docker compose down -v
```

## 트러블슈팅

### Maven 빌드 실패
- `mvn dependency:tree`로 의존성 충돌 확인
- Java 21 호환성 확인 (`--enable-preview` 플래그 필요 여부)

### Docker 빌드 실패
- 각 서비스 `Dockerfile` 존재 확인 (현재 `docker-compose.yml`에서 `build: ./서비스명` 사용)
- 멀티모듈 프로젝트이므로 부모 pom.xml에서 버전 관리 확인

### 통합 테스트 타임아웃
- GitHub Actions 러너 리소스 제한 (7GB RAM, 2 CPU) 고려
- 헬스체크 대기 시간 증가 필요 시 `timeout-minutes` 조정

### K8s 검증 실패
- `kubeconform` 버전과 클러스터 버전 매칭 확인
- CRD 누락 시 `--skip` 옵션으로 제외 가능

## 모니터링 및 관찰성

파이프라인 실행 결과는 다음에서 확인 가능:
- **GitHub Actions 탭**: 워크플로우 실행 히스토리, 로그, 아티팩트
- **Security 탭**: Trivy 취약점 스캔 결과
- **Packages 탭**: Docker 이미지 버전, 다운로드 수
- **Environments 탭**: Staging/Production 배포 이력, 승인 대기

---

**관련 문서**:
- [Kubernetes 배포 가이드](../k8s/README.md)
- [Schema Registry 가이드](../docs/schema-registry-guide.md)
- [아키텍처 문서](../docs/architecture.md)