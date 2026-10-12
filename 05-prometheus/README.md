# 05-prometheus

Prometheus + node-exporter 를 Docker Compose 로 띄워 메트릭을 수집하고 PromQL 로 조회한다.

## 실행

```bash
docker compose up -d
```

## 확인

- Targets: http://localhost:9090/targets  (두 job 모두 `UP`)
- Query:   http://localhost:9090/graph

## 정리

```bash
docker compose down -v   # -v: 수집 데이터 볼륨까지 삭제
```

관련 글: posts/001-prometheus-metrics-quickstart.md
