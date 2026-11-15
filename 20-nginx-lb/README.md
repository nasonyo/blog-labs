# 20-nginx-lb

Nginx 로 백엔드 3대(traefik/whoami)에 로드밸런싱하며 라운드 로빈, weight, zone, 서버 장애를 확인한 실습.

- 환경: Docker Compose, nginx:1.27-alpine, traefik/whoami:v1.10.3, 호스트 포트 8090
- 이 Mac 은 CPU 10개라 Nginx 워커도 10개였다. 결과는 이 한 번의 실행 기준이다.

## 실행
```bash
docker compose up -d
cp conf/zone.conf nginx.conf && docker compose restart lb
curl localhost:8090
```

## 설정 파일
- `conf/nozone.conf`: 기본 upstream
- `conf/zone.conf`: `zone backend 64k;` 추가
- `conf/weighted.conf`: zone + `app1 weight=3`

## 로그
- `logs/01-roundrobin.txt`: zone 유무와 weight 에 따른 분배
- `logs/02-one-down.txt`, `03-one-down-retry.txt`: 서버 1대 중지, 느린 요청과 재시도
- `logs/04-all-down.txt`: 전부 중지(502)와 복구
