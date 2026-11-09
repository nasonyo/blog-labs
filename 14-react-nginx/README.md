# 14-react-nginx

메모 API 위에 **React(Vite + TypeScript) 화면**을 만들고, **Nginx 로 서빙하면서 `/api` 를 백엔드로 프록시**한다.
브라우저 → Nginx → Spring Boot → PostgreSQL 의 풀스택을 `docker compose up` 한 번으로 띄운다.

백엔드는 [10-spring-postgres/app](../10-spring-postgres/app) 을 그대로 쓴다. (이 디렉터리에 복사하지 않았다)

## 구성

```
14-react-nginx/
├── compose.yaml              # db + app(Spring Boot) + web(Nginx + React 빌드 결과)
└── frontend/
    ├── Dockerfile            # 멀티 스테이지: Node 로 빌드 -> Nginx 로 서빙
    ├── nginx.conf            # /api 프록시, SPA 라우팅 폴백, 정적 파일 캐시
    ├── package.json / package-lock.json
    ├── vite.config.ts        # 개발 서버의 /api 프록시
    └── src/                  # React 앱 (목록/추가/삭제, 상세 페이지)
```

## 준비물

- Docker Desktop (Node.js 는 컨테이너 안에서 빌드하므로 필요 없다. 개발 서버를 쓰려면 Node 20 이상)

## 실행

```bash
docker compose up -d --build
docker compose ps
```

- 화면: http://localhost:8085
- 백엔드 직접 접근(디버깅용): http://localhost:8090/api/memos

### 개발 서버 (선택)

```bash
cd frontend
npm ci
npm run dev        # http://localhost:5173, /api 는 localhost:8090 으로 프록시
```

백엔드(`app`)가 `8090` 으로 떠 있어야 한다. (`docker compose up -d app db`)

### API 주소를 바꿔서 빌드하기

```bash
VITE_API_BASE=http://localhost:8090 docker compose build web
```

기본값(빈 문자열)은 "같은 주소의 `/api`" 이다. 값을 지정하면 브라우저가 다른 주소로 직접 요청하게 되어 **CORS** 가 필요해진다. (글에서 이 에러를 재현한다)

## 정리

```bash
docker compose down -v
```

## 주의

- 실습용이다. 백엔드 포트(8090)를 공개한 것은 디버깅 편의 때문이고, 운영에서는 공개하지 않는다.
- Nginx 설정의 `proxy_pass http://app:8080` 은 `app` 이라는 이름이 **시작 시점에** 해석되어야 한다. compose 네트워크 밖에서 이 이미지를 단독으로 실행하면 Nginx 가 시작되지 않는다.
- 포트: web `8085`, app `8090`. kind 클러스터가 `8080` 을 쓰고 있을 수 있어서 피했다.
