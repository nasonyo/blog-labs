# 13-jenkins-k8s

Jenkins 파이프라인으로 **이미지 빌드 → kind 클러스터에 적재 → 쿠버네티스에 롤링 배포 → 스모크 테스트** 를 자동화한다.
배포가 실패하면 직전 버전으로 **자동 롤백**한다.

> ⚠️ **실습용 구성이다. 운영 환경에서 그대로 쓰면 안 된다.**
> - Jenkins 컨테이너가 `root` 로 실행되고 호스트의 `/var/run/docker.sock` 을 마운트한다. (호스트 Docker 를 사실상 root 로 제어)
> - `kubeconfig` 에 **클러스터 관리자 인증서**가 들어 있고, Jenkins 가 클러스터 전체를 제어할 수 있다.
> - 관리자 계정 기본값이 `admin` / `admin` 이다.

## 구성

```
13-jenkins-k8s/
├── compose.yaml          # Jenkins (docker.sock, kubeconfig 마운트, kind 네트워크 연결)
├── Jenkinsfile           # Validate → Build Image → Load to Cluster → Deploy → Smoke Test
├── jenkins/
│   ├── Dockerfile        # jenkins:lts + docker CLI + kubectl + kind + 플러그인
│   └── casc.yaml         # JCasC: 관리자 계정, 파이프라인 잡(memo-deploy) 자동 등록
└── .gitignore            # kubeconfig 는 커밋하지 않는다
```

배포 대상은 [12-k8s-spring-postgres](../12-k8s-spring-postgres) 의 `memo-app` Deployment 이고, 이미지는 [11-spring-monitoring/app](../11-spring-monitoring/app) 에서 빌드한다.

## 준비물

- Docker Desktop, `kubectl`, `kind`
- **12-k8s-spring-postgres 가 클러스터에 배포되어 있을 것** (namespace `memo`, DB, Secret 포함)
- kind 클러스터 이름이 `blog` 일 것 (다르면 `Jenkinsfile` 의 `KIND_NAME` 과 아래 명령을 수정)

## 실행

```bash
# 1) Jenkins 가 클러스터에 접속할 kubeconfig (kind 네트워크 안에서 쓰는 주소로 생성)
kind get kubeconfig --name blog --internal > kubeconfig
chmod 600 kubeconfig

# 2) Jenkins 시작 (첫 빌드는 이미지를 받느라 몇 분 걸린다)
docker compose up -d --build
```

- Jenkins: http://localhost:9080 (`admin` / `admin`)
- 잡 이름: `memo-deploy`

### 첫 빌드는 "Build Now" 로

`BREAK_DEPLOY` 파라미터는 `Jenkinsfile` 안에 선언되어 있어서, **처음 한 번 실행되기 전에는 Jenkins 가 그 존재를 모른다.** 첫 빌드는 `Build Now`(API 는 `/job/memo-deploy/build`)로 실행하고, 그 뒤부터 `Build with Parameters` 를 쓴다. (실제로 첫 빌드를 `buildWithParameters` 로 요청했더니 HTTP 400 으로 거절됐다)

### 배포 실패와 롤백 확인

`Build with Parameters` → `BREAK_DEPLOY` 체크 → 존재하지 않는 이미지 태그로 배포가 실패하고, `post { failure }` 에서 `kubectl rollout undo` 가 실행된다.

```bash
kubectl -n memo get pods                 # 옛 Pod 는 계속 Running
kubectl -n memo get deploy memo-app -o jsonpath='{.spec.template.spec.containers[0].image}'
```

## 정리

```bash
docker compose down -v        # Jenkins 컨테이너와 볼륨
rm kubeconfig
```

## 주의

- 포트: Jenkins `9080`. (`8080` 은 kind 의 NodePort 매핑을 통한 앱 접근에 쓰인다)
- `kind load docker-image` 는 kind 전용이다. 실제 환경에서는 컨테이너 레지스트리에 푸시하고 클러스터가 pull 한다.
