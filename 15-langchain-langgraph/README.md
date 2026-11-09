# 15-langchain-langgraph

**LangChain** 과 **LangGraph** 로 AI 에이전트를 각각 하나씩 만든다. LLM 은 **Gemini API** 를 쓴다.

| 스크립트 | 프레임워크 | 하는 일 |
|---|---|---|
| `agents/langchain_agent.py` | LangChain (`create_agent`) | LLM 이 도구(로그 조회, 배포 이력)를 스스로 골라 호출하며 질문에 답한다 |
| `agents/langgraph_workflow.py` | LangGraph (`StateGraph`) | 장애 로그를 분류 → 분석 → **사람 승인** → 롤백(시뮬레이션) → 보고서 순서로 처리한다 |

## 구성

```
15-langchain-langgraph/
├── compose.yaml                  # 에이전트 실행용 (profile: run). 두 서비스가 이미지 하나를 공유
├── Dockerfile / requirements.txt # 버전 고정 (langchain 1.4.9, langgraph 1.2.20, langchain-google-genai 4.4.3)
├── .env.example                  # API 키 예시 (.env 로 복사해서 사용, .env 는 커밋하지 않는다)
├── agents/
│   ├── llm.py                    # 모델 설정 (Gemini). 다른 모델로 바꿀 때 수정하는 곳
│   ├── tools.py                  # 두 에이전트가 공유하는 도구 3개
│   ├── langchain_agent.py
│   └── langgraph_workflow.py
└── data/                         # 샘플 데이터 (실습용으로 만든 가짜 로그와 배포 이력)
    ├── logs/memo-app.log
    ├── logs/web.log
    └── deploy_history.json
```

> `data/` 의 로그와 배포 이력은 **실습용으로 만든 샘플**이다. 실제 서비스 로그가 아니다.
> 롤백 단계는 **시뮬레이션**이다. 명령 문자열을 만들 뿐 실제로 실행하지 않는다. 도구는 모두 읽기 전용이다.
> 실행하면 샘플 로그의 내용이 **Gemini API 로 전송**된다. 실제 서비스 로그를 쓸 때는 민감 정보(개인정보, 토큰 등)를 먼저 제거한다.

## 준비물

- Docker Desktop
- [Google AI Studio](https://aistudio.google.com/) 에서 발급한 Gemini API 키

```bash
cp .env.example .env        # .env 를 열어서 GOOGLE_API_KEY=발급받은키 로 바꾼다
```

`.env` 는 `.gitignore` 에 들어 있어서 커밋되지 않는다. **키를 채팅, 이슈, 코드에 붙여넣지 않는다.**

사용할 수 있는 모델은 계정마다 다르다. 목록은 아래로 확인한다.

```bash
set -a; . ./.env; set +a
curl -s "https://generativelanguage.googleapis.com/v1beta/models?pageSize=200" -H "x-goog-api-key: $GOOGLE_API_KEY"
```

기본 모델은 `gemini-3.5-flash` 다. 바꾸려면 `MODEL=모델이름 docker compose ...` 로 실행한다.

## 실행

```bash
# LangChain 에이전트
docker compose --profile run run --rm --build langchain-agent
docker compose --profile run run --rm langchain-agent python -m agents.langchain_agent "web 서비스는 이상 없어?"

# LangGraph 워크플로 (service: memo-app | web, decision: approve | reject | ask)
docker compose --profile run run --rm langgraph-workflow python -m agents.langgraph_workflow memo-app approve
docker compose --profile run run --rm langgraph-workflow python -m agents.langgraph_workflow memo-app reject
docker compose --profile run run --rm langgraph-workflow python -m agents.langgraph_workflow web approve

# 그래프 구조를 Mermaid 로 출력
docker compose --profile run run --rm langgraph-workflow python -m agents.langgraph_workflow memo-app approve --graph
```

터미널에서 직접 승인하려면 `decision` 에 `ask` 를 주고 `-it` 로 실행한다.

### 분류 재시도 루프 확인 (테스트용)

```bash
docker compose --profile run run --rm -e SIMULATE_CLASSIFY_FAILURES=2 langgraph-workflow \
  python -m agents.langgraph_workflow memo-app reject
```

앞의 N 번의 분류를 일부러 실패로 처리해서 `classify` 노드가 재시도되는 것을 본다. (3회를 넘기면 안전하게 `high` 로 취급한다)

## 자주 만나는 문제

| 증상 | 원인과 대응 |
|---|---|
| `404 NOT_FOUND ... no longer available to new users` | 계정에서 더 이상 쓸 수 없는 모델이다. 위의 모델 목록에서 다른 모델을 고른다 |
| `503 UNAVAILABLE ... high demand` | 모델이 과부하다. `llm.py` 의 `timeout`, `max_retries` 로 대기 시간을 제한해 두었다. 잠시 뒤 재시도하거나 다른 모델을 쓴다 |
| `429 RESOURCE_EXHAUSTED` | 무료 한도(분당 호출 수 등)를 넘었다. 1분쯤 기다렸다가 실행한다 |
| `.env` 가 없다는 오류 | `cp .env.example .env` 후 키를 채운다 |

## 정리

```bash
docker compose down
docker rmi langchain-langgraph-lab:local
rm .env
```

## 주의

- LLM 응답은 실행마다 조금씩 다를 수 있다.
- 보고서 같은 LLM 생성 문장은 "시뮬레이션" 같은 단서를 빼먹을 수 있다. 중요한 사실은 코드로 확인한다.
- 에이전트에게 쓰기 권한이 있는 도구를 줄 때는 반드시 승인 절차나 권한 제한을 둔다.
