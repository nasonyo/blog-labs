# blog-labs

기술 블로그(Tistory) 글에 사용한 실습 코드 모음. 각 디렉토리는 `docker compose up -d` 또는 `kubectl apply` 로 재현됩니다.

| 디렉토리 | 주제 | 블로그 글 |
|---|---|---|
| [05-prometheus](./05-prometheus) | Docker Compose로 메트릭 수집 시작하기 | TODO: Tistory URL |

## 규칙
- 시크릿은 커밋하지 않는다. 필요한 값은 `.env.example` 로 제공한다.
- 이미지 태그는 버전을 고정한다 (`latest` 금지).
