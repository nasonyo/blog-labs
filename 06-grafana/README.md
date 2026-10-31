# 06-grafana

Prometheus 의 메트릭을 Grafana 대시보드로 시각화한다. 데이터소스와 대시보드는 파일(provisioning)로 자동 등록된다.

## 사전 준비
- 로컬 머신에서 node_exporter 실행 (`brew services start node_exporter`)
- `05-prometheus` 가 떠 있다면 포트 충돌을 피하기 위해 먼저 내린다: `docker compose down`

## 실행
```bash
docker compose up -d
```

- Grafana: http://localhost:3000  (admin / admin)
- Prometheus: http://localhost:9090

## 구성
| 파일 | 역할 |
|---|---|
| `provisioning/datasources/prometheus.yml` | Prometheus 를 데이터소스로 자동 등록 |
| `provisioning/dashboards/dashboards.yml` | 대시보드 JSON 폴더를 지정 |
| `dashboards/local-machine.json` | CPU / 메모리 / 디스크 패널 4개 |

## 정리
```bash
docker compose down -v
```
