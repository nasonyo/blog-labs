# 09-kubernetes

kind 로 로컬 Kubernetes 클러스터(컨트롤 플레인 1 + 워커 2)를 만들고, Flask 샘플 앱을 **Deployment + Service** 로 배포한다.
자가 복구, 확장, 롤링 업데이트, 롤백, 서비스 이름(DNS) 접근까지 실습한다.

## 구성

```
09-kubernetes/
├── kind-config.yaml                 # 클러스터 정의 (노드 3개, localhost:8080 -> NodePort 30080)
├── app/                             # 샘플 앱 (Flask). 응답에 버전과 Pod 이름을 포함
│   ├── app.py
│   ├── requirements.txt
│   └── Dockerfile                   # --build-arg APP_VERSION 으로 버전 주입
├── k8s/
│   ├── deployment.yaml              # replicas 3, readinessProbe, resources
│   └── service.yaml                 # NodePort 30080
└── extra/
    └── deployment-graceful.yaml     # preStop 을 추가한 버전 (롤링 업데이트 중 실패 감소)
```

## 준비물

- Docker Desktop (실행 중), `kubectl`, `kind` (`brew install kind`)
- Docker Desktop 메모리 4GB 이상 권장 (이 글은 8GB 환경에서 실행)

## 실행

```bash
# 1) 이미지 두 개 빌드 (v1, v2)
docker build -t demo-app:v1 --build-arg APP_VERSION=v1 app
docker build -t demo-app:v2 --build-arg APP_VERSION=v2 app

# 2) 클러스터 생성 + 이미지를 노드에 적재
kind create cluster --name blog --config kind-config.yaml
kind load docker-image demo-app:v1 demo-app:v2 --name blog

# 3) 배포
kubectl apply -f k8s/
kubectl rollout status deployment/demo-app

# 4) 확인
curl localhost:8080/
```

## 정리

```bash
kind delete cluster --name blog
docker rmi demo-app:v1 demo-app:v2    # (선택)
```

## 주의

- 실습용 구성이다. NodePort 와 로컬 이미지 적재는 학습 편의를 위한 것이다.
- kind 클러스터를 삭제하면 클러스터 안의 모든 리소스가 함께 사라진다.
