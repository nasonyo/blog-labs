# 17-mcp-memo-server

앞선 글들에서 만든 **메모 서비스(REST API)를 AI 가 도구로 쓸 수 있게 해 주는 MCP 서버**다.
AI 코딩 도구(Claude Code 등)가 "메모 목록 조회", "태그로 검색", "새 메모 만들기"를 할 수 있게 된다.

| MCP 요소 | 이름 | 설명 |
|---|---|---|
| 도구 (읽기 전용) | `list_memos(tag?)`, `get_memo(memo_id)`, `list_tags()` | 조회 |
| 도구 (쓰기) | `create_memo(title, content?, tags?)` | 메모 생성. **삭제 도구는 일부러 만들지 않았다** |
| 리소스 | `memo://{memo_id}` | 메모 하나를 읽기 좋은 텍스트로 |
| 프롬프트 | `summarize_memos(tag?)` | 요약을 요청하는 문장 |

## 구성

```
17-mcp-memo-server/
├── compose.yaml          # 메모 API(db + app) + MCP 서버 이미지 빌드 + 점검 컨테이너
└── server/
    ├── server.py         # MCP 서버 (mcp 2.3.0, stdio 전송)
    ├── check.py          # MCP 클라이언트 코드로 서버를 직접 호출해 보는 점검 스크립트
    ├── Dockerfile
    └── requirements.txt  # mcp==2.3.0, httpx==0.28.1
```

메모 API 는 [16-speckit-memo-tags](../16-speckit-memo-tags/app) 의 앱(태그 기능 포함)을 그대로 빌드해서 쓴다.

## 준비물

- Docker Desktop
- (Claude Code 에 연결해 보려면) Claude Code CLI

## 실행

```bash
# 1) 메모 API 실행 (localhost:8090) 과 MCP 서버 이미지 빌드
docker compose up -d --build app
docker compose --profile build build mcp-memo        # 이미지: mcp-memo:local

# 2) MCP 클라이언트 코드로 서버 점검 (tools/list, tools/call, resources/read, prompts/get)
docker compose --profile run run --rm -e PYTHONUNBUFFERED=1 mcp-check 2>/dev/null
#   2>/dev/null 은 서버가 stderr 로 남기는 로그를 숨긴다. 로그까지 보려면 빼면 된다
```

## Claude Code 에 연결

```bash
# 기본(local) 범위: 나만, 이 프로젝트에서만 쓴다
claude mcp add memo -- docker run -i --rm \
  --add-host=host.docker.internal:host-gateway \
  -e MEMO_API_URL=http://host.docker.internal:8090 \
  mcp-memo:local

claude mcp list          # memo ... ✔ Connected
claude mcp get memo
# Claude Code 안에서는 /mcp 로 상태를 본다. 도구 이름은 mcp__memo__list_memos 처럼 보인다
```

- `--` 앞은 Claude Code 의 옵션, 뒤는 **서버를 실행하는 명령**이다.
- `--scope project` 로 등록하면 `.mcp.json` 에 저장되어 팀과 공유되지만, 처음에는 **"승인 대기(Pending approval)"** 상태가 된다.
- Docker CLI 가 PATH 에 없으면(Docker Desktop 기본 설치에서 있을 수 있다) `docker` 대신 전체 경로를 쓴다.

## 정리

```bash
claude mcp remove memo
docker compose down -v
docker rmi mcp-memo:local
```

## 주의

- 실습용이다. 메모 API 에는 인증이 없고, MCP 서버는 그 API 를 그대로 노출한다.
- `server.py` 는 stdio 서버이므로 **stdout 에 출력(`print`)하지 않는다.** 로그는 stderr 로 쓴다.
- 도구에서 예상 가능한 실패는 `ToolError` 로 던져야 오류 메시지가 AI 에게 전달된다 (글 참고).
- `mcp` 패키지 2.x 에서는 `FastMCP` 가 `MCPServer` 로 바뀌었다. 1.x 용 튜토리얼 코드는 그대로 동작하지 않을 수 있다.
