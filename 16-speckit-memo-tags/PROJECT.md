# 메모 서비스 (Spec Kit 로 개발한 태그 기능 포함)

Spring Boot 메모 REST API. 태그 기능은 [specs/001-memo-tags](./specs/001-memo-tags) 의 명세에서 출발해서 만들었다.

## API 변경 사항 (태그 기능)

기존 엔드포인트는 경로와 기존 필드를 바꾸지 않았다. 추가된 것은 다음과 같다.

| 변경 | 내용 |
|---|---|
| `POST /api/memos` | 선택 필드 `tags` (문자열 배열, 최대 10개, 각 1~30자). 앞뒤 공백 제거, 소문자 정규화, 중복 제거 |
| 응답 | 모든 메모에 `tags` 추가 (이름순, 없으면 `[]`) |
| `GET /api/memos?tag=이름` | 해당 태그가 붙은 메모만. 없는 태그는 `[]`. 한글 값은 URL 인코딩이 필요하다 (`curl -G --data-urlencode`) |
| `GET /api/tags` | `[{"name","count"}]`, 이름순 |
| 400 응답 | 기존 필드(`timestamp`, `status`, `error`, `path`)는 유지하고 `errors[{field,message}]` 추가 |

자세한 계약은 [contracts/api.md](./specs/001-memo-tags/contracts/api.md).

## 실행과 테스트 (Docker 만 필요)

```bash
# 자동화 테스트 (29건)
docker run --rm -v "$PWD/app":/build -v m2cache:/root/.m2 -w /build maven:3.9.9-eclipse-temurin-17 mvn -B test

# 실제 PostgreSQL 로 실행
docker compose up -d --build
```

검증 시나리오는 [quickstart.md](./specs/001-memo-tags/quickstart.md) 에 있다.
