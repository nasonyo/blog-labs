# 12-k8s-spring-postgres

Spring Boot 메모 API 와 PostgreSQL 을 **쿠버네티스(kind)** 에 배포한다.
ConfigMap/Secret 으로 설정을 분리하고, StatefulSet + PVC 로 DB 데이터를 보존한다.

앱 소스는 [11-spring-monitoring/app](../11-spring-monitoring/app) 을 그대로 쓴다. (이 디렉터리에 복사하지 않았다)

## 구성

```
12-k8s-spring-postgres/
└── k8s/
    ├── 00-namespace.yaml          # Namespace: memo
    ├── 10-configmap.yaml          # 민감하지 않은 설정 (DB 이름, JDBC URL 등)
    ├── 11-secret.example.yaml     # Secret 예시 (실제 값은 kubectl 로 직접 생성)
    ├── 20-postgres.yaml           # 헤드리스 Service + StatefulSet(+PVC)
    └── 30-app.yaml                # Deployment(initContainer, probes) + Service(NodePort 30080)
```

## 준비물

- Docker Desktop, `kubectl`, `kind`
- [09-kubernetes](../09-kubernetes) 의 클러스터 설정 (노드 3개, `localhost:8080` → NodePort `30080`)

## 실행

```bash
# 1) 클러스터 (이미 만들었다면 생략)
kind create cluster --name blog --config ../09-kubernetes/kind-config.yaml

# 2) 이미지 빌드 + 클러스터에 적재
docker build -t memo-app:v1 ../11-spring-monitoring/app
kind load docker-image memo-app:v1 --name blog

# 3) 네임스페이스와 설정
kubectl apply -f k8s/00-namespace.yaml -f k8s/10-configmap.yaml

# 4) Secret 은 파일이 아니라 명령으로 만든다 (비밀번호는 원하는 값으로)
kubectl -n memo create secret generic memo-secret --from-literal=POSTGRES_PASSWORD='원하는-비밀번호'

# 5) DB 와 앱 배포
kubectl apply -f k8s/20-postgres.yaml -f k8s/30-app.yaml
kubectl -n memo rollout status statefulset/postgres
kubectl -n memo rollout status deployment/memo-app

# 6) 확인
curl localhost:8080/actuator/health
curl -X POST localhost:8080/api/memos -H 'Content-Type: application/json' \
     -d '{"title":"첫 메모","content":"쿠버네티스에서"}'
curl localhost:8080/api/memos
```

> 8080 포트의 NodePort `30080` 이 다른 Service(예: 09-kubernetes 의 `demo-app`)와 겹치면 안 된다. 먼저 `kubectl delete -f ../09-kubernetes/k8s/` 로 지운다.

## 정리

```bash
kubectl delete namespace memo      # 앱, DB, PVC(데이터) 모두 삭제
kind delete cluster --name blog    # (선택) 클러스터 삭제
```

## 주의

- 실습용이다. DB 가 단일 Pod 이고 백업/이중화가 없다. NodePort, 로컬 이미지 적재도 학습 편의를 위한 것이다.
- Secret 은 base64 인코딩일 뿐 암호화가 아니다. 실제 비밀번호를 파일로 커밋하지 않는다.
