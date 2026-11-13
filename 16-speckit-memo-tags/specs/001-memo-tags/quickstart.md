# Quickstart: 메모 태그 검증

로컬에는 Docker 만 있으면 된다.

## 1. 자동화 테스트

```bash
docker run --rm -v "$PWD/app":/build -w /build maven:3.9.9-eclipse-temurin-17 mvn -q -B test
```

기대 결과: 모든 테스트 통과 (`Tests run: N, Failures: 0, Errors: 0`).

## 2. 실제 스택에서 확인 (PostgreSQL)

```bash
docker compose up -d --build
```

### P1: 태그를 붙여 만든다

```bash
curl -s -X POST localhost:8090/api/memos -H 'Content-Type: application/json' \
  -d '{"title":"주간 회의","content":"안건","tags":["회의","Weekly"," 회의 "]}'
# 기대: 201, "tags":["회의","weekly"]  (중복 제거, 소문자, 공백 제거)
```

### 호환성: 기존 방식 요청

```bash
curl -s -X POST localhost:8090/api/memos -H 'Content-Type: application/json' -d '{"title":"태그 없음"}'
# 기대: 201, "tags":[]
```

### P2: 태그로 거른다

```bash
curl -s 'localhost:8090/api/memos?tag=Weekly'                      # 기대: "주간 회의" 만
curl -s -G localhost:8090/api/memos --data-urlencode 'tag=없는태그'   # 기대: []
curl -s 'localhost:8090/api/memos'                                  # 기대: 전체

# 주의: 한글 태그는 URL 에 그대로 쓰면 서버(Tomcat)가 HTTP 400 HTML 을 돌려준다.
# 퍼센트 인코딩이 필요하고, curl 에서는 -G --data-urlencode 를 쓰면 된다. (브라우저와 HTTP 클라이언트 라이브러리는 자동으로 인코딩한다)
```

### P3: 태그 목록

```bash
curl -s localhost:8090/api/tags    # 기대: [{"name":"weekly","count":1},{"name":"회의","count":1}]
```

### 검증 오류

```bash
curl -s -X POST localhost:8090/api/memos -H 'Content-Type: application/json' -d '{"title":"x","tags":[""]}'
# 기대: 400, errors[0].field = "tags[0]"
```
