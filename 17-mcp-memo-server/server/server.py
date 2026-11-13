"""메모 서비스를 AI 가 다룰 수 있게 해 주는 MCP 서버.

도구(tools): list_memos, get_memo, list_tags (읽기 전용), create_memo (쓰기)
리소스(resources): memo://{memo_id}
프롬프트(prompts): summarize_memos

주의: stdio 전송에서는 표준 출력(stdout)이 MCP 메시지 전용 통로다.
      로그는 반드시 stderr 로 쓰고, print() 를 쓰지 않는다.
"""
import os
import sys

import httpx
from mcp.server import MCPServer
from mcp.server.mcpserver.exceptions import ResourceNotFoundError, ToolError
from mcp.types import ToolAnnotations

API_URL = os.getenv("MEMO_API_URL", "http://localhost:8090").rstrip("/")

mcp = MCPServer(
    "memo",
    instructions="메모 서비스(REST API)를 조회하고 새 메모를 만든다. 삭제 기능은 제공하지 않는다.",
)


def log(message: str) -> None:
    print(f"[memo-mcp] {message}", file=sys.stderr, flush=True)


def call_api(method: str, path: str, **kwargs):
    """메모 API 를 호출한다.

    예상할 수 있는 실패(연결 불가, 404, 400)는 ToolError 로 던진다. ToolError 의 메시지는 AI 에게 그대로 전달되어
    AI 가 읽고 입력을 고쳐서 다시 시도할 수 있다. 그 밖의 예외는 "충돌"로 취급되어 메시지가 AI 에게 숨겨진다.
    """
    try:
        response = httpx.request(method, f"{API_URL}{path}", timeout=10, **kwargs)
    except httpx.HTTPError as e:
        raise ToolError(f"메모 API({API_URL})에 연결하지 못했다: {type(e).__name__}") from e
    if response.status_code == 404:
        raise ToolError("요청한 메모를 찾을 수 없다")
    if response.status_code == 400:
        errors = response.json().get("errors", [])
        detail = "; ".join(f"{e['field']}: {e['message']}" for e in errors) or "잘못된 요청"
        raise ToolError(f"입력이 올바르지 않다 ({detail})")
    response.raise_for_status()
    return response.json()


# ---------------------------------------------------------------- tools

@mcp.tool(annotations=ToolAnnotations(readOnlyHint=True))
def list_memos(tag: str | None = None) -> list[dict]:
    """메모 목록을 돌려준다. tag 를 주면 그 태그가 붙은 메모만 돌려준다."""
    log(f"list_memos tag={tag!r}")
    params = {"tag": tag} if tag else None
    return call_api("GET", "/api/memos", params=params)


@mcp.tool(annotations=ToolAnnotations(readOnlyHint=True))
def get_memo(memo_id: int) -> dict:
    """메모 하나를 id 로 조회한다."""
    log(f"get_memo id={memo_id}")
    return call_api("GET", f"/api/memos/{memo_id}")


@mcp.tool(annotations=ToolAnnotations(readOnlyHint=True))
def list_tags() -> list[dict]:
    """사용 중인 태그와 각 태그가 붙은 메모 수를 돌려준다."""
    log("list_tags")
    return call_api("GET", "/api/tags")


@mcp.tool(annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False))
def create_memo(title: str, content: str = "", tags: list[str] | None = None) -> dict:
    """새 메모를 만든다. 제목은 필수(100자 이하), 태그는 최대 10개(각 30자 이하)이며 소문자로 저장된다."""
    log(f"create_memo title={title!r} tags={tags!r}")
    return call_api("POST", "/api/memos", json={"title": title, "content": content, "tags": tags or []})


# ---------------------------------------------------------------- resource

@mcp.resource("memo://{memo_id}")
def memo_resource(memo_id: int) -> str:
    """메모 하나의 내용을 읽기 좋은 텍스트로 돌려준다."""
    try:
        memo = call_api("GET", f"/api/memos/{memo_id}")
    except ToolError as e:
        raise ResourceNotFoundError(str(e)) from e   # 리소스에서는 리소스용 오류를 쓴다
    tags = ", ".join(memo.get("tags", [])) or "(없음)"
    return f"# {memo['title']}\n\n태그: {tags}\n\n{memo.get('content') or ''}"


# ---------------------------------------------------------------- prompt

@mcp.prompt()
def summarize_memos(tag: str | None = None) -> str:
    """메모를 요약해 달라고 AI 에게 요청하는 문장을 만든다."""
    scope = f"'{tag}' 태그가 붙은 메모" if tag else "모든 메모"
    return f"list_memos 도구로 {scope}를 가져와서, 주제별로 묶어 3줄 이내로 요약해 줘. 메모에 없는 내용은 쓰지 마."


if __name__ == "__main__":
    log(f"starting (api={API_URL})")
    mcp.run()  # 기본 전송 방식은 stdio
