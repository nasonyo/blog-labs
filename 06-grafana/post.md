지난 글에서는 node_exporter 로 로컬 머신의 메트릭을 수집하고 PromQL 로 조회했다. 숫자는 잘 나오지만 매번 쿼리를 입력해서 보는 건 불편하다. 이번에는 **Grafana** 를 붙여서 메트릭을 대시보드로 보여준다.

대시보드를 손으로 만들면 컨테이너를 지울 때마다 설정도 같이 날아간다. 그래서 데이터소스와 대시보드를 **파일로 정의(프로비저닝)** 해서 `docker compose up` 한 번으로 똑같이 재현되게 만들고, 마지막에는 grafana.com 에 공개된 대시보드를 **Import** 해서 써본다.

실습 코드: https://github.com/nasonyo/blog-labs/tree/main/06-grafana

---

## 1. Prometheus 와 Grafana 의 역할

| | 역할 |
|---|---|
| Prometheus | 메트릭을 수집·저장하고 PromQL 로 조회에 응답한다 |
| Grafana | 데이터소스(Prometheus 등)에 쿼리를 보내고 결과를 패널·대시보드로 그린다 |

Grafana 는 데이터를 저장하지 않는다. "어디서 가져올지(데이터소스)"와 "어떻게 그릴지(대시보드)"만 알고 있고, 실제 데이터는 Prometheus 에 질의해서 가져온다.

```
node_exporter ──(scrape)──▶ Prometheus ◀──(PromQL)── Grafana ◀── 브라우저
 (로컬 머신)                  (컨테이너)               (컨테이너)
```

## 2. 구성

```
06-grafana/
├── compose.yaml
├── prometheus.yml
├── provisioning/
│   ├── datasources/prometheus.yml   # 데이터소스 자동 등록
│   └── dashboards/dashboards.yml    # 대시보드 JSON 폴더 지정
└── dashboards/
    └── local-machine.json           # 패널 4개짜리 대시보드
```

### compose.yaml

```yaml
services:
  prometheus:
    image: prom/prometheus:v2.53.0
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    command:
      - --config.file=/etc/prometheus/prometheus.yml
      - --storage.tsdb.retention.time=7d
    extra_hosts:
      - "host.docker.internal:host-gateway"

  grafana:
    image: grafana/grafana:11.1.0
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD:-admin}
      GF_AUTH_ANONYMOUS_ENABLED: ${GF_AUTH_ANONYMOUS_ENABLED:-false}
      GF_AUTH_ANONYMOUS_ORG_ROLE: ${GF_AUTH_ANONYMOUS_ORG_ROLE:-Viewer}
    volumes:
      - ./provisioning:/etc/grafana/provisioning:ro   # 데이터소스·대시보드를 파일로 자동 등록
      - ./dashboards:/etc/dashboards:ro               # 대시보드 JSON
      - grafana-data:/var/lib/grafana                 # Grafana 자체 데이터(사용자·설정) 보존
    depends_on:
      - prometheus

volumes:
  prometheus-data:
  grafana-data:
```

- Grafana 는 `/etc/grafana/provisioning` 아래 파일을 기동할 때 읽어서 데이터소스와 대시보드를 등록한다.
- 로그인 계정은 실습용 기본값(`admin` / `admin`)이다. 비밀번호는 `GRAFANA_ADMIN_PASSWORD` 환경변수로 바꿀 수 있다. 실제 환경에서는 기본값을 그대로 쓰면 안 된다.
- 이미지 태그는 버전을 고정했다.

### 데이터소스 프로비저닝

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    uid: prometheus                    # 대시보드 JSON 에서 이 uid 로 참조한다
    type: prometheus
    access: proxy                      # Grafana 서버가 대신 조회 (브라우저 → Grafana → Prometheus)
    url: http://prometheus:9090        # compose 서비스 이름으로 접근
    isDefault: true
```

- `url` 은 `localhost` 가 아니라 compose 서비스 이름(`prometheus`)이다. Grafana 컨테이너 안에서 `localhost` 는 Grafana 자기 자신이기 때문이다.
- `uid` 를 고정해 두면 대시보드 JSON 이 이 값으로 데이터소스를 참조할 수 있어서, 어느 환경에서든 같은 대시보드가 동작한다.

### 대시보드 프로비저닝

```yaml
apiVersion: 1

providers:
  - name: local-machine
    type: file
    allowUiUpdates: true
    options:
      path: /etc/dashboards               # 이 폴더의 JSON 을 대시보드로 등록
```

`/etc/dashboards` 에 있는 JSON 파일이 대시보드로 등록된다. `allowUiUpdates: true` 라서 UI 에서 수정은 되지만, 저장은 DB 에만 되고 원본 파일은 바뀌지 않는다.

### 대시보드 패널 (local-machine.json)

패널 4개로 구성했고, 쿼리는 지난 글에서 쓴 PromQL 이다.

| 패널 | PromQL |
|---|---|
| CPU 사용률 | `100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100)` |
| CPU 상태(mode)별 | `sum by (mode) (rate(node_cpu_seconds_total[1m]))` |
| 메모리 free 비율 | `node_memory_free_bytes / node_memory_total_bytes * 100` |
| 디스크 사용률 (/) | `(1 - node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}) * 100` |

> 메트릭 이름은 운영체제마다 다르다. 위 쿼리는 macOS 의 node_exporter 기준이다(`node_memory_free_bytes` 등). Linux 에서는 `node_memory_MemFree_bytes`, `node_memory_MemTotal_bytes` 처럼 이름이 다르니 바꿔서 써야 한다.

## 3. 실행

사전 준비로 로컬 머신에서 node_exporter 가 실행 중이어야 한다.

```bash
brew install node_exporter
brew services start node_exporter
curl -s localhost:9100/metrics | head
```

이전 실습(`05-prometheus`)이 떠 있다면 포트·컨테이너 이름이 겹치므로 먼저 내린다.

```bash
docker compose up -d
```

- Grafana: http://localhost:3000 (admin / admin)
- Prometheus: http://localhost:9090

`http://localhost:9090/targets` 에서 `node` 와 `prometheus` job 이 모두 `UP` 인지 확인한다.

## 4. 확인

### 데이터소스

Connections → Data sources 에 Prometheus 가 들어 있고, 파일로 등록했기 때문에 "Provisioned data source … cannot be modified using the UI" 안내가 보인다.

![프로비저닝된 데이터소스](https://raw.githubusercontent.com/nasonyo/blog-labs/main/06-grafana/images/01-datasource.png)

### 대시보드

Dashboards 에 `Local Machine (node_exporter)` 가 자동으로 등록돼 있다. 우리가 직접 만든 패널 4개다.

![Local Machine 대시보드](https://raw.githubusercontent.com/nasonyo/blog-labs/main/06-grafana/images/02-dashboard.png)

터미널에서도 같은 내용을 확인할 수 있다.

```bash
curl -s -u admin:admin localhost:3000/api/datasources
curl -s -u admin:admin 'localhost:3000/api/search?type=dash-db'
curl -s -u admin:admin localhost:3000/api/datasources/uid/prometheus/health
# {"message":"Successfully queried the Prometheus API.","status":"OK"}
```

## 5. 공개 대시보드 Import 하기

직접 패널을 만드는 것도 좋지만, node_exporter 처럼 널리 쓰이는 exporter 는 이미 잘 만들어진 대시보드가 공개돼 있다. [grafana.com/dashboards](https://grafana.com/grafana/dashboards/) 에서 ID 로 가져올 수 있다. 여기서는 **Node Exporter Full (ID 1860)** 을 써본다.

1. Dashboards → New → Import 로 이동한다 (`/dashboard/import`).
2. `Grafana.com dashboard URL or ID` 에 `1860` 을 입력하고 Load 를 누른다.

![ID 입력](https://raw.githubusercontent.com/nasonyo/blog-labs/main/06-grafana/images/03-import-id.jpg)

3. 이름·폴더를 확인하고 Import 를 누른다.

![Import 옵션](https://raw.githubusercontent.com/nasonyo/blog-labs/main/06-grafana/images/04-import-options.jpg)

> Import 는 Grafana 서버가 grafana.com 에서 JSON 을 내려받는 방식이라 인터넷이 필요하다. 오프라인이면 JSON 파일을 직접 업로드하면 된다.

Import 가 끝나면 상단의 Datasource / Job / Nodename / Instance 변수가 자동으로 채워진다. Datasource 는 우리가 프로비저닝한 `Prometheus`, Job 은 `node`, Instance 는 `host.docker.internal:9100` 이다.

![Node Exporter Full](https://raw.githubusercontent.com/nasonyo/blog-labs/main/06-grafana/images/05-node-exporter-full.jpg)

### macOS 에서는 일부 패널이 "No data"

Memory, RAM, SWAP, Pressure 같은 패널이 비어 있다. 이 대시보드는 **Linux 의 node_exporter 메트릭**(`node_memory_MemTotal_bytes` 등)을 기준으로 만들어졌는데, macOS 는 `node_memory_total_bytes` 처럼 이름이 다르거나 아예 없는 메트릭이 있기 때문이다. 실제로 Prometheus 에 질의해 보면 이 메트릭이 없다.

```bash
curl -s 'localhost:9090/api/v1/query?query=node_memory_MemTotal_bytes'
# result: []  (series 0개)
```

CPU, 네트워크, 디스크처럼 양쪽에 공통인 패널은 정상적으로 나온다. Linux 서버에서는 대부분 그대로 쓸 수 있고, macOS 에서는 우리가 만든 `Local Machine` 대시보드의 메모리 패널과 같이 쓰면 된다.

### 프로비저닝 vs Import

| | 프로비저닝 | Import |
|---|---|---|
| 정의 위치 | 파일(코드)로 관리 | Grafana DB |
| 재현성 | `compose up` 만으로 동일하게 복원 | 컨테이너·볼륨을 지우면 다시 해야 함 |
| 용도 | 팀 공통 대시보드, 환경 재현 | 공개 대시보드를 빠르게 시험 |

Import 한 대시보드를 계속 쓰려면 JSON 을 내려받아서 `dashboards/` 에 넣으면 프로비저닝으로 바꿀 수 있다. 다만 공개 대시보드 JSON 은 `${DS_PROMETHEUS}` 같은 변수를 포함하는 경우가 있어서 데이터소스 uid 에 맞춰 손봐야 할 수 있다.

## 6. 정리

```bash
docker compose down -v          # 컨테이너 + 볼륨(Prometheus·Grafana 데이터) 삭제
brew services stop node_exporter  # (선택) node_exporter 중지
```

`-v` 를 붙이면 Import 한 대시보드도 함께 사라진다. 프로비저닝한 데이터소스·대시보드는 다음 `up` 때 파일에서 다시 만들어진다.

## 마무리

Prometheus 가 모은 메트릭을 Grafana 에서 대시보드로 볼 수 있게 됐다. 데이터소스와 대시보드를 파일로 정의해 두니 환경을 지웠다 다시 띄워도 똑같이 복원된다. 공개 대시보드는 Import 로 바로 써볼 수 있지만, 운영체제나 exporter 버전에 따라 일부 패널이 비어 있을 수 있다는 점은 알아둬야 한다.

메트릭은 여기까지고, 로그는 ELK 스택으로 다뤘다. 함께 보면 좋다: https://journal52709.tistory.com/2
