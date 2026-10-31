로그와 메트릭 다음은 **배포 파이프라인**이다. 코드를 푸시하면 테스트하고, 이미지를 만들고, 배포하고, 정상인지 확인하는 과정을 자동화하는 도구로 CI/CD 에서 가장 오래 쓰여 온 **Jenkins** 를 다룬다.

이번 글은 두 부분으로 나뉜다. 앞부분에서는 Jenkins 가 어떻게 생겨났고 왜 지금도 많은 기업에서 쓰이는지 살펴보고, 뒷부분에서는 Docker Compose 로 Jenkins 를 직접 띄워서 파이프라인을 돌려본다.

실습 코드: https://github.com/nasonyo/blog-labs/tree/main/08-jenkins

> ⚠️ **이 글의 구성은 실습용이다.** Jenkins 컨테이너에 호스트의 `docker.sock` 을 마운트하고 root 로 실행하며, 관리자 계정 기본값은 `admin` / `admin` 이다. 학습·로컬 용도로만 쓰고 운영 환경에 그대로 적용하면 안 된다. 이유는 뒤에서 설명한다.

---

## 1. CI/CD 와 Jenkins 의 역할

- **CI (Continuous Integration)** — 코드가 변경될 때마다 빌드와 테스트를 자동으로 수행해서 문제를 빨리 발견한다.
- **CD (Continuous Delivery / Deployment)** — 검증된 결과물을 배포 가능한 상태로 만들거나 실제 환경까지 자동으로 배포한다.

Jenkins 는 이 흐름을 **파이프라인(여러 단계의 작업 묶음)** 으로 정의하고 실행하는 자동화 서버다. 소스 저장소에서 코드를 가져오고, 각 단계를 순서대로 실행하고, 결과를 기록해서 보여준다. 빌드 도구, 테스트 프레임워크, 배포 대상이 무엇이든 "실행할 수 있는 명령"으로 연결할 수 있다는 점이 핵심이다.

## 2. Jenkins 의 역사

| 시기 | 사건 |
|---|---|
| 2005년경 | Sun Microsystems 의 개발자 **Kohsuke Kawaguchi** 가 **Hudson** 이라는 이름으로 개발해 첫 릴리스 |
| 2010년대 초 | Oracle 이 Sun 을 인수. Hudson 이름(상표) 문제를 두고 Oracle 과 커뮤니티가 갈등 |
| 2011년 1월 | 커뮤니티 투표로 프로젝트 이름을 **Jenkins** 로 변경 (1월 11일 투표 시작, 29일 승인) |
| 2011년 2월 | Oracle 이 Hudson 개발을 계속하겠다고 밝히면서 Jenkins 를 포크로 간주. 두 프로젝트가 갈라짐. 이후 Hudson 은 Eclipse 재단으로 넘어감 |
| 2016년 4월 | **Jenkins 2.0** 출시. **Pipeline** 플러그인이 기본으로 활성화되고, Groovy 기반 DSL 로 파이프라인을 코드로 작성 |
| 2019년 | Jenkins 가 **Continuous Delivery Foundation(CD Foundation)** 의 창립 프로젝트로 합류 |

핵심은 두 가지 전환이다.

1. **커뮤니티가 이름을 지키고 계속 발전시켰다.** 상표 분쟁으로 갈라진 뒤에도 커뮤니티 중심의 개발은 Jenkins 쪽으로 이어졌고, 지금의 Jenkins 가 됐다. Kawaguchi 는 이후 CloudBees 에 합류해 Jenkins 의 상용 지원 생태계에도 기여했다.
2. **UI 클릭에서 코드로 넘어갔다.** 초기에는 웹 화면에서 잡(job)을 하나씩 설정했지만, Jenkins 2.0 의 Pipeline 과 `Jenkinsfile` 덕분에 파이프라인을 소스 저장소에 코드로 두고 버전 관리하는 방식(Pipeline as Code)이 표준이 됐다. 이 글의 실습도 그 방식이다.

## 3. 왜 아직도 많은 기업에서 Jenkins 를 쓸까

GitHub Actions, GitLab CI 같은 서비스형 CI 가 많아졌는데도 Jenkins 는 여전히 현업에서 널리 쓰인다. 일반적으로 꼽히는 이유는 다음과 같다.

**1) 방대한 플러그인 생태계**

공식 플러그인 인덱스(plugins.jenkins.io)에는 **2,300개 이상**의 커뮤니티 플러그인이 올라와 있다. 소스 저장소, 빌드 도구, 클라우드, 알림, 보안 스캔, 배포 도구 등 대부분의 연동을 플러그인으로 해결할 수 있다. 오래 쓰인 만큼 "이미 누군가 만들어 둔 것"이 많다는 점이 가장 큰 자산이다.

**2) 어디서든 직접 운영할 수 있다**

Jenkins 는 MIT 라이선스의 오픈소스이고 직접 설치해서 운영한다. 폐쇄망이나 온프레미스처럼 외부 SaaS 를 쓰기 어려운 환경, 데이터와 실행 환경을 직접 통제해야 하는 조직에서 선택지가 된다.

**3) 유연성**

Pipeline 은 Groovy 기반이라 조건 분기, 반복, 병렬 실행, 공유 라이브러리 같은 복잡한 흐름도 표현할 수 있다. 빌드 노드(에이전트)도 물리 서버, VM, 컨테이너 어디에든 붙일 수 있다.

**4) 쌓아 온 자산과 전환 비용**

이미 수백, 수천 개의 파이프라인과 플러그인 설정이 쌓인 조직에서는 이를 다른 도구로 옮기는 비용이 크다. 잘 돌아가는 CI 를 굳이 바꾸지 않는 경우가 많다.

**5) 지금도 활발히 개발되고 있다**

프로젝트는 CD Foundation 아래에서 계속 유지·개발되고 있고, 정기적으로 LTS 버전이 나온다. 이번 실습에서 쓰는 이미지도 `2.541.3` LTS 다. 설정을 코드로 관리하는 **JCasC(Jenkins Configuration as Code)** 같은 현대적인 기능도 꾸준히 추가돼 왔다.

**솔직하게 짚어야 할 아쉬운 점**

- **운영 부담이 있다.** 서버 업그레이드, 플러그인 호환성 확인, 보안 패치를 직접 챙겨야 한다. 서비스형 CI 는 이런 부분을 대신 해준다.
- **플러그인이 양날의 검이다.** 편리한 만큼 플러그인 간 호환성, 유지보수가 멈춘 플러그인, 보안 취약점을 관리해야 한다. 실제로 이번 실습에서도 최신 플러그인이 더 새로운 Jenkins 버전을 요구해서 설치가 실패했고, LTS 버전을 올려서 해결했다.
- **오래된 구조다.** Groovy 파이프라인은 러닝커브가 있고, 신생 도구에 비해 UI 와 설정 방식이 무겁게 느껴질 수 있다.

정리하면 Jenkins 는 "가장 새로운" 도구가 아니라 **"가장 많은 환경에 맞출 수 있는" 도구**다. 새 프로젝트라면 서비스형 CI 가 더 가벼울 수 있지만, 기존 자산이 많거나 직접 통제가 필요한 환경에서는 여전히 강력한 선택지다. DevOps 엔지니어라면 한 번은 제대로 다뤄 봐야 하는 이유이기도 하다.

## 4. 이번 데모의 범위

Docker Compose 로 Jenkins 를 띄우고, 샘플 웹앱(Flask)을 **테스트 → 이미지 빌드 → 배포 → 스모크 테스트** 하는 파이프라인을 실행한다.

```
저장소(GitHub) ──▶ Jenkins (컨테이너) ──docker.sock──▶ 호스트 Docker
                    │  1. Test        docker build --target test   (pytest)
                    │  2. Build       docker build --target runtime
                    │  3. Deploy      docker run demo-app (cicd-net)
                    └─ 4. Smoke Test  curl http://demo-app:8000/health
```

- Jenkins 설정(관리자 계정, 파이프라인 잡)은 **JCasC** 로 코드에 넣어서, `docker compose up` 한 번으로 똑같이 재현된다. 06 Grafana 글에서 본 프로비저닝과 같은 접근이다.
- Jenkins 컨테이너 안에서 호스트의 Docker 를 쓰기 위해 **docker CLI 만 설치**하고 호스트의 `docker.sock` 을 마운트한다. 그래서 앱 컨테이너는 Jenkins 안이 아니라 **호스트 Docker** 에서 실행된다(형제 컨테이너 방식).

### 왜 "실습용" 인가

`docker.sock` 을 마운트하면 Jenkins 안에서 실행되는 코드가 호스트 Docker 를 제어할 수 있다. 이는 사실상 호스트의 root 권한과 같다. 누군가 파이프라인이나 플러그인을 악용하면 호스트 전체가 위험해진다. 운영 환경에서는 격리된 빌드 에이전트나 Kaniko, Podman 같은 데몬리스 빌드 도구를 쓰는 편이 안전하다. 이 글에서는 흐름을 이해하는 데 집중하려고 가장 단순한 방식을 택했다.

## 5. 구성

```
08-jenkins/
├── compose.yaml            # Jenkins 컨테이너 (docker.sock 마운트, cicd-net 네트워크)
├── jenkins/
│   ├── Dockerfile          # jenkins:lts + docker CLI + 플러그인
│   └── casc.yaml           # JCasC: 관리자 계정, 파이프라인 잡 자동 등록
└── app/                    # 배포 대상 샘플 앱 (Flask)
    ├── Jenkinsfile         # Test → Build → Deploy → Smoke Test
    ├── Dockerfile          # test / runtime 멀티 스테이지
    ├── app.py
    └── tests/test_app.py
```

### compose.yaml

```yaml
# [실습용 구성] 아래 설정은 로컬 학습 목적이며 운영 환경에서 그대로 쓰면 안 된다.
services:
  jenkins:
    build: ./jenkins
    image: lab-jenkins:2.541.3
    container_name: jenkins
    user: root
    ports:
      - "8080:8080"
    environment:
      JENKINS_ADMIN_PASSWORD: ${JENKINS_ADMIN_PASSWORD:-admin}
      JOB_REPO_URL: ${JOB_REPO_URL:-https://github.com/nasonyo/blog-labs.git}
    volumes:
      - jenkins-home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock   # 호스트 Docker 데몬 사용 (실습용)
    networks:
      - cicd

networks:
  cicd:
    name: cicd-net        # 배포되는 앱 컨테이너도 이 네트워크에 붙여서 이름으로 접근한다

volumes:
  jenkins-home:
```

앱 컨테이너를 같은 네트워크(`cicd-net`)에 붙이면, 스모크 테스트에서 Jenkins 가 `http://demo-app:8000` 처럼 **컨테이너 이름으로** 앱을 호출할 수 있다.

### jenkins/Dockerfile

```dockerfile
FROM jenkins/jenkins:2.541.3-lts-jdk17

USER root

# 호스트 Docker 데몬을 호출하기 위한 docker CLI 만 복사한다 (데몬은 설치하지 않는다)
COPY --from=docker:27.1.1-cli /usr/local/bin/docker /usr/local/bin/docker

# [실습용] 마운트된 로컬 저장소의 소유자가 달라도 git 이 동작하도록 허용
RUN git config --system --add safe.directory '*'

# 플러그인 설치 (JCasC, Job DSL, Pipeline, Git, Pipeline Stage View)
RUN jenkins-plugin-cli --plugins configuration-as-code job-dsl workflow-aggregator git pipeline-stage-view

# 초기 설정 마법사 생략, JCasC 설정 파일 위치 지정
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
ENV CASC_JENKINS_CONFIG=/var/jenkins_home/casc.yaml
COPY casc.yaml /var/jenkins_home/casc.yaml
```

플러그인은 이미지 빌드 시점에 `jenkins-plugin-cli` 로 설치한다. 이 글에서는 플러그인 버전을 고정하지 않고 최신 호환 버전을 받는다(운영에서는 고정을 권장한다).

### jenkins/casc.yaml (JCasC)

```yaml
jenkins:
  systemMessage: "08-jenkins 실습용 Jenkins (운영 환경에서 사용 금지)"
  securityRealm:
    local:
      allowsSignup: false
      users:
        - id: admin
          password: ${JENKINS_ADMIN_PASSWORD:-admin}   # 실습용 기본값
  authorizationStrategy:
    loggedInUsersCanDoAnything:
      allowAnonymousRead: false

jobs:
  - script: >
      pipelineJob('demo-app') {
        definition {
          cpsScm {
            scm {
              git {
                remote { url('${JOB_REPO_URL:-https://github.com/nasonyo/blog-labs.git}') }
                branches('*/main')
              }
            }
            scriptPath('08-jenkins/app/Jenkinsfile')
            lightweight(true)
          }
        }
      }
```

로그인 사용자 설정과 잡 정의가 모두 파일에 있다. 컨테이너를 지웠다 다시 띄워도 UI 에서 다시 설정할 필요가 없다.

### app/Dockerfile — 테스트를 이미지 빌드에 포함

```dockerfile
# --- test 스테이지: 테스트가 실패하면 이미지 빌드가 여기서 멈춘다 ---
FROM python:3.12-slim AS test
WORKDIR /app
COPY requirements.txt requirements-dev.txt ./
RUN pip install --no-cache-dir -r requirements.txt -r requirements-dev.txt
COPY . .
RUN python -m pytest -q

# --- runtime 스테이지: 테스트 도구 없이 실행에 필요한 것만 ---
FROM python:3.12-slim AS runtime
WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .
EXPOSE 8000
CMD ["gunicorn", "-b", "0.0.0.0:8000", "app:app"]
```

테스트를 별도 컨테이너로 돌리면서 소스를 볼륨으로 마운트하면, Jenkins 컨테이너 안의 경로가 호스트 Docker 에는 존재하지 않아서 마운트가 깨진다(`docker.sock` 방식의 대표적인 함정이다). 그래서 **테스트를 멀티 스테이지 Dockerfile 의 `test` 스테이지에 넣었다**. 소스가 `COPY` 로 이미지에 들어가기 때문에 이 문제가 없고, 테스트가 실패하면 빌드 자체가 멈춘다.

### app/Jenkinsfile

```groovy
pipeline {
    agent any

    environment {
        IMAGE = 'demo-app'
        NETWORK = 'cicd-net'
    }

    stages {
        stage('Test') {
            steps {
                // Dockerfile 의 test 스테이지에서 pytest 를 실행한다
                dir('08-jenkins/app') {
                    sh 'docker build --target test -t ${IMAGE}:test .'
                }
            }
        }

        stage('Build') {
            steps {
                dir('08-jenkins/app') {
                    sh 'docker build --target runtime -t ${IMAGE}:${BUILD_NUMBER} -t ${IMAGE}:latest .'
                }
            }
        }

        stage('Deploy') {
            steps {
                // 같은 이름의 기존 컨테이너를 교체한다
                sh '''
                    docker rm -f ${IMAGE} 2>/dev/null || true
                    docker run -d --name ${IMAGE} --network ${NETWORK} -p 8000:8000 ${IMAGE}:${BUILD_NUMBER}
                '''
            }
        }

        stage('Smoke Test') {
            steps {
                // Jenkins 컨테이너에서 같은 네트워크의 앱 컨테이너를 이름으로 호출한다
                sh '''
                    for i in 1 2 3 4 5 6 7 8 9 10; do
                        curl -sf http://${IMAGE}:8000/health && exit 0
                        sleep 1
                    done
                    echo "health check failed"; exit 1
                '''
            }
        }
    }
}
```

## 6. 실행

```bash
docker compose up -d --build
```

- Jenkins: http://localhost:8080 (admin / admin)
- 로그인하면 `demo-app` 잡이 이미 등록돼 있다. JCasC 가 만든 것이다.

본인 저장소(fork)로 실습하려면 `JOB_REPO_URL` 로 주소를 바꿔서 띄운다.

```bash
JOB_REPO_URL=https://github.com/<본인계정>/blog-labs.git docker compose up -d --build
```

`demo-app` → **Build Now** 를 누르면 파이프라인이 실행된다.

## 7. 결과 확인

### 성공한 빌드

Stage View 에서 Test, Build, Deploy, Smoke Test 단계가 모두 초록색으로 통과한 것을 볼 수 있다. 아래 화면은 여러 번 실행한 이력이며, 맨 위 #5 가 최신 성공 빌드다.

![Stage View](https://raw.githubusercontent.com/nasonyo/blog-labs/main/08-jenkins/images/01-stage-view.jpg)

콘솔 로그에서는 배포와 스모크 테스트 단계가 이렇게 찍힌다.

```
+ docker rm -f demo-app
+ docker run -d --name demo-app --network cicd-net -p 8000:8000 demo-app:3
+ curl -sf http://demo-app:8000/health
{"status":"ok"}
Deployed demo-app:3
Finished: SUCCESS
```

호스트에서도 배포된 앱이 응답한다.

```bash
curl -s localhost:8000/
# {"message":"Hello from demo-app","version":"1.0.0"}

docker ps --filter name=demo-app
# demo-app   demo-app:5   Up ...
```

빌드할 때마다 `demo-app:<빌드번호>` 태그가 붙고, 실행 중인 컨테이너의 이미지 태그를 보면 어떤 빌드가 배포됐는지 알 수 있다.

### 일부러 테스트를 깨뜨려 보기

CI 의 핵심은 **실패했을 때 배포를 막는 것**이다. 테스트의 기대값을 일부러 틀리게 바꿔서 커밋한 뒤 빌드해 봤다(Stage View 의 빨간 #4).

```
>       assert res.get_json()["message"] == "Hello BROKEN"
E       AssertionError: assert 'Hello from demo-app' == 'Hello BROKEN'
FAILED tests/test_app.py::test_index
1 failed, 1 passed in 0.08s
Stage "Build" skipped due to earlier failure(s)
Stage "Deploy" skipped due to earlier failure(s)
Stage "Smoke Test" skipped due to earlier failure(s)
```

- Test 단계에서 실패하자 Build, Deploy, Smoke Test 는 **건너뛰었다**.
- 실행 중이던 이전 컨테이너(`demo-app:3`)는 **그대로 유지**됐다. 깨진 코드가 배포되지 않은 것이다.
- 테스트를 원래대로 되돌린 뒤 다시 빌드하자 #5 가 성공하고 `demo-app:5` 가 배포됐다.

## 8. 트러블슈팅 (실제로 겪은 것)

- **플러그인 설치 실패** — `requires a greater version of Jenkins`. 최신 플러그인이 더 새로운 Jenkins 를 요구했다. 베이스 이미지를 최신 LTS 로 올려서 해결했다. 플러그인 호환성 관리가 필요하다는 앞의 이야기와 같은 사례다.
- **`file://` 로컬 저장소 체크아웃 거부** — 로컬 디렉터리를 Git 저장소로 쓰면 Jenkins 가 보안상 거부한다. 이 글의 기본 설정은 GitHub 주소라서 문제가 없다.
- **테스트 컨테이너 볼륨 마운트가 깨지는 문제** — 위에서 설명한 대로, 테스트를 Dockerfile 의 `test` 스테이지로 옮겨서 피했다.

## 9. 정리

```bash
docker rm -f demo-app            # 파이프라인이 배포한 컨테이너 (compose 밖에서 만들어진다)
docker compose down -v           # Jenkins 컨테이너 + 볼륨 + cicd-net 네트워크
docker rmi demo-app:latest       # (선택) 빌드된 앱 이미지
```

`demo-app` 컨테이너는 compose 가 만든 것이 아니라서, 먼저 지워야 `cicd-net` 네트워크가 정상적으로 삭제된다.

## 마무리

Jenkins 를 Docker Compose 로 띄우고, 설정과 잡을 코드(JCasC, Jenkinsfile)로 정의해서 `테스트 → 빌드 → 배포 → 스모크 테스트` 파이프라인을 돌려봤다. 테스트가 실패하면 배포가 막히는 CI 의 핵심 동작도 확인했다.

Jenkins 는 오래됐고 관리 부담도 있지만, 플러그인 생태계와 유연성, 직접 통제할 수 있다는 점 때문에 지금도 현업에서 널리 쓰이는 도구다. 이번 구성은 실습용으로 단순화했으니, 실제 환경에서는 빌드 에이전트 분리, 자격 증명 관리, 플러그인 버전 고정 같은 부분을 반드시 보강해야 한다.

다음에는 Git 푸시를 트리거로 자동 빌드되는 구성이나, Jenkins 의 메트릭을 Prometheus·Grafana 로 모니터링하는 구성으로 이어갈 수 있다. 앞선 글들과 함께 보면 좋다.

- [Prometheus] 메트릭 수집: https://journal52709.tistory.com/1
- [ELK] 로그 수집·조회: https://journal52709.tistory.com/2
- [Grafana] 대시보드: https://journal52709.tistory.com/3

**참고**
- Jenkins (software) - Wikipedia: https://en.wikipedia.org/wiki/Jenkins_(software)
- Jenkins plugin index: https://plugins.jenkins.io/
- Jenkins Pipeline docs: https://www.jenkins.io/doc/book/pipeline/
