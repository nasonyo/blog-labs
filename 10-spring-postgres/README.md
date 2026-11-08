# 10-spring-postgres

Spring Boot(REST API + JPA) 앱과 PostgreSQL 을 Docker Compose 로 함께 띄운다.
메모를 저장/조회/삭제하는 API 를 만들고, 컨테이너를 재시작해도 데이터가 유지되는지 확인한다.

## 구성

```
10-spring-postgres/
├── compose.yaml              # db(PostgreSQL) + app(Spring Boot)
├── .env.example              # DB 접속 정보 예시 (복사해서 .env 로 사용)
└── app/
    ├── Dockerfile            # 멀티 스테이지 (Maven 빌드 -> JRE 실행)
    ├── pom.xml
    └── src/main/
        ├── java/com/example/memo/   # Memo(엔티티), MemoRepository, MemoController
        └── resources/application.yml
```

## 준비물

- Docker Desktop (JDK, Maven 은 필요 없다. 빌드는 컨테이너 안에서 한다)

## 실행

```bash
cp .env.example .env        # (선택) 값을 바꾸고 싶을 때. 없으면 기본값(memo)을 쓴다
docker compose up -d --build
docker compose ps

curl localhost:8090/actuator/health
curl -X POST localhost:8090/api/memos -H 'Content-Type: application/json' \
     -d '{"title":"첫 메모","content":"Spring Boot 와 PostgreSQL"}'
curl localhost:8090/api/memos
```

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/memos` | 메모 생성 (title 필수, 100자 이하) |
| GET | `/api/memos` | 전체 조회 |
| GET | `/api/memos/{id}` | 단건 조회 (없으면 404) |
| DELETE | `/api/memos/{id}` | 삭제 (없으면 404, 성공 204) |
| GET | `/actuator/health` | 헬스체크 |

## 정리

```bash
docker compose down        # 컨테이너 삭제, 데이터(볼륨)는 유지
docker compose down -v     # 볼륨까지 삭제 (데이터 삭제)
```

## 주의

- 실습용 구성이다. `ddl-auto: update`, 기본 비밀번호(`memo`), DB 포트 미공개 등은 학습 편의를 위한 것이다.
- 앱 포트는 호스트 `8090` 이다. (8080, 8081 은 다른 실습과 겹칠 수 있어서 피했다)
