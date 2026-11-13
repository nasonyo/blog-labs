# 16-speckit-memo-tags

**GitHub Spec Kit** 으로 기존 메모 REST API([10-spring-postgres](../10-spring-postgres) 의 앱)에 **태그 기능**을 추가한 기록이다.
명세(spec) → 정리(clarify) → 계획(plan) → 작업(tasks) → 분석(analyze) → 구현(implement) → 점검(converge) 순서로 진행했고,
각 단계의 산출물이 그대로 들어 있다.

> 이 디렉터리는 **Spec Kit 을 사용한 결과물**이다. 프로젝트 자체의 사용법(API, 실행, 테스트)은 [PROJECT.md](./PROJECT.md) 를 본다.

## 구성

```
16-speckit-memo-tags/
├── .specify/                      # Spec Kit 이 만든 틀 (specify init)
│   ├── memory/constitution.md     # 프로젝트 헌법 (직접 작성한 원칙 5개)
│   ├── templates/                 # spec, plan, tasks, checklist 템플릿
│   ├── scripts/bash/              # 단계별 보조 스크립트
│   └── workflows/speckit/         # 승인 게이트가 있는 전체 흐름 정의
├── specs/001-memo-tags/           # 이 기능의 문서들
│   ├── spec.md                    # 무엇을, 왜 (사용자 스토리, 요구사항, 정리된 질문)
│   ├── plan.md  research.md  data-model.md  contracts/api.md  quickstart.md
│   ├── tasks.md                   # 38개 작업 (테스트 먼저)
│   ├── analysis-report.md         # /speckit-analyze 결과
│   ├── converge-report.md         # /speckit-converge 결과
│   └── checklists/requirements.md # 명세 품질 점검표
├── app/                           # 태그 기능이 구현된 Spring Boot 앱 (테스트 29건)
├── compose.yaml
├── PROJECT.md                     # 서비스 README (API 변경, 실행, 테스트)
└── git-log.txt                    # 작업 순서가 남은 커밋 기록 (18개)
```

> `.claude/skills/` (Spec Kit 이 Claude Code 용으로 설치하는 명령 파일 9개)는 용량 때문에 제외했다. 아래 명령으로 다시 만들 수 있다.

## 같은 틀을 직접 만들어 보기

```bash
# uv 가 필요하다 (https://docs.astral.sh/uv/). 이 글에서는 Docker 컨테이너 안에서 실행했다.
uv tool install specify-cli            # 이 글에서 쓴 버전: 1.4.0
specify init my-project --integration claude
cd my-project
# Claude Code 에서:
#   /speckit-constitution  /speckit-specify  /speckit-clarify  /speckit-plan
#   /speckit-tasks  /speckit-analyze  /speckit-implement  /speckit-converge
```

Docker 안에서 시도해 보려면:

```bash
docker run --rm -v "$PWD":/work -w /work python:3.12-slim sh -c '
  apt-get update -qq && apt-get install -y -qq git >/dev/null
  pip install -q uv && uv tool install specify-cli
  export PATH=$PATH:/root/.local/bin
  specify init demo --integration claude --ignore-agent-tools --non-interactive'
```

`--ignore-agent-tools` 는 컨테이너에 Claude Code CLI 가 없어서 에이전트 확인 단계를 건너뛰는 옵션이다.

## 실행과 테스트

```bash
# 자동화 테스트 (29건, Docker 만 필요)
docker run --rm -v "$PWD/app":/build -v m2cache:/root/.m2 -w /build maven:3.9.9-eclipse-temurin-17 mvn -B test

# 실제 PostgreSQL 로 실행
docker compose up -d --build
```

## 주의

- 이 기록은 **에이전트(Claude)가 Spec Kit 스킬 파일의 지시를 읽고 단계를 수행**한 것이다. 슬래시 명령을 자동 실행한 것이 아니고,
  `/speckit-clarify` 의 질문에 대한 답은 글을 쓰려고 사람 대신 정했다. 실제 팀에서는 사람이 답하고 단계마다 검토해야 한다.
- 일부 단계(git 브랜치 생성 hook 등)는 생략했다.
- 실습용 구성이다. 기본 비밀번호(`memo`)와 `ddl-auto: update` 는 학습 편의를 위한 것이다.
