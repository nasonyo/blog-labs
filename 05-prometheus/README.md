# 05-prometheus

로컬 머신에서 실행 중인 node_exporter 를 컨테이너 안의 Prometheus 가 수집하고, PromQL 로 조회한다.

## 사전 준비: node_exporter 실행 (로컬 머신)

```bash
# macOS
brew install node_exporter
brew services start node_exporter

# 확인
curl -s localhost:9100/metrics | head
```

Linux 는 릴리스 바이너리를 내려받아 `./node_exporter` 로 실행한다.

## 실행

```bash
docker compose up -d
```

## 확인

- Targets: http://localhost:9090/targets  (`node` job 이 `UP`)
- Query:   http://localhost:9090/graph

## 정리

```bash
docker compose down -v          # Prometheus 컨테이너 + 볼륨 삭제
brew services stop node_exporter  # (선택) node_exporter 중지
```

관련 글: posts/001-prometheus-metrics-quickstart.md
