# 08-jenkins

Docker Compose 로 Jenkins 를 띄우고, Jenkinsfile 파이프라인으로 **테스트 → 이미지 빌드 → 배포 → 스모크 테스트** 를 수행한다.

> ⚠️ **실습용 구성이다. 운영 환경에서 그대로 쓰면 안 된다.**
> - Jenkins 컨테이너가 `root` 로 실행되고 호스트의 `/var/run/docker.sock` 을 마운트한다.
>   Jenkins 에서 실행되는 코드는 호스트 Docker 를 사실상 root 권한으로 제어할 수 있다.
> - 관리자 계정 기본값이 `admin` / `admin` 이다. (`JENKINS_ADMIN_PASSWORD` 로 변경 가능)
> - 플러그인은 최신 호환 버전이 설치된다 (이미지 태그만 고정).

## 구성

```
08-jenkins/
├── compose.yaml            # Jenkins 컨테이너 (docker.sock 마운트, cicd-net 네트워크)
├── jenkins/
│   ├── Dockerfile          # jenkins:lts + docker CLI + 플러그인
│   └── casc.yaml           # JCasC: 관리자 계정, 파이프라인 잡(demo-app) 자동 등록
└── app/                    # 배포 대상 샘플 앱 (Flask)
    ├── Jenkinsfile         # Test → Build → Deploy → Smoke Test
    ├── Dockerfile          # test / runtime 멀티 스테이지
    ├── app.py
    └── tests/test_app.py
```

## 실행

```bash
docker compose up -d --build
```

- Jenkins: http://localhost:8080  (admin / admin)
- `demo-app` 잡은 JCasC(Job DSL)로 자동 등록된다. 이 저장소의 `08-jenkins/app/Jenkinsfile` 을 읽는다.
- 본인 저장소(fork)를 쓰려면: `JOB_REPO_URL=https://github.com/<you>/blog-labs.git docker compose up -d --build`

## 파이프라인 실행

UI 에서 `demo-app` → **Build Now**.

```bash
# 배포 결과 확인
curl -s localhost:8000/          # {"message":"Hello from demo-app","version":"1.0.0"}
curl -s localhost:8000/health    # {"status":"ok"}
```

테스트가 실패하면 Build / Deploy / Smoke Test 단계는 건너뛰고, 기존에 배포된 컨테이너는 그대로 유지된다.

## 정리

```bash
docker rm -f demo-app            # 파이프라인이 배포한 컨테이너 (compose 밖에서 만들어진다)
docker compose down -v           # Jenkins 컨테이너 + 볼륨 + cicd-net 네트워크
docker rmi demo-app:latest       # (선택) 빌드된 앱 이미지
```

## 트러블슈팅
- 플러그인 설치가 `requires a greater version of Jenkins` 로 실패하면 `jenkins/Dockerfile` 의 Jenkins LTS 태그를 올린다.
- 빌드 단계에서 `docker: command not found` / `permission denied` 이면 docker CLI 복사와 `docker.sock` 마운트, `user: root` 설정을 확인한다.
