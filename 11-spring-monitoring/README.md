# 11-spring-monitoring

Spring Boot 앱(메모 API + PostgreSQL)에 **Micrometer 메트릭**을 붙이고, **Prometheus 로 수집 → Grafana 대시보드**로 보는 구성.
앞 단계 [10-spring-postgres](../10-spring-postgres) 의 앱을 기반으로, 의존성 1개와 설정 몇 줄이 추가된다.

## 구성

```
11-spring-monitoring/
├── compose.yaml                         # db + app + prometheus + grafana
├── load.sh                              # 부하 생성기 (201/200/404/400 을 섞어서 호출)
├── .env.example
├── app/                                 # 10-spring-postgres 와 같은 앱 (+ micrometer-registry-prometheus)
├── prometheus/prometheus.yml            # app:8080/actuator/prometheus 를 5초마다 수집
└── grafana/
    ├── provisioning/                    # 데이터소스·대시보드 자동 등록
    └── dashboards/memo-app.json         # 패널 8개
```

### 10-spring-postgres 대비 바뀐 점

| 파일 | 변경 |
|---|---|
| `app/pom.xml` | `micrometer-registry-prometheus` 의존성 추가 |
| `app/src/main/resources/application.yml` | `prometheus` 엔드포인트 노출, 공통 태그 `application=memo`, `http.server.requests` 히스토그램 활성화 |
| `compose.yaml` | `prometheus`, `grafana` 서비스 추가 |

## 실행

```bash
docker compose up -d --build
docker compose ps

curl -s localhost:8090/actuator/prometheus | head      # 메트릭 원문
./load.sh 120                                          # 120초 동안 부하 생성
```

- Prometheus: http://localhost:9090 (Status → Targets 에서 `memo-app` 이 UP 인지 확인)
- Grafana: http://localhost:3000 (admin / admin) → Dashboards → **Memo App (Spring Boot)**

로그인 없이 보려면 (데모용):

```bash
GF_AUTH_ANONYMOUS_ENABLED=true GF_AUTH_ANONYMOUS_ORG_ROLE=Viewer docker compose up -d grafana
```

## 정리

```bash
docker compose down -v
```

## 주의

- 실습용 구성이다. Grafana 기본 계정(`admin`/`admin`), 짧은 수집 주기(5초)는 학습 편의를 위한 것이다.
- `/actuator/prometheus` 는 인증 없이 열려 있다. 운영에서는 관리 포트를 분리하거나 접근을 제한한다.
- 포트: app `8090`, Prometheus `9090`, Grafana `3000`. 다른 실습 스택과 동시에 띄우지 않는다.
