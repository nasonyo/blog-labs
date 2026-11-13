"""MCP 클라이언트로 서버를 직접 호출해 보는 점검 스크립트.
Claude Code 같은 호스트가 하는 일(서버 실행 → 목록 조회 → 도구 호출)을 코드로 보여준다."""
import asyncio
import json
import os
import sys
import time

import httpx
from mcp import Client, StdioServerParameters

API_URL = os.getenv("MEMO_API_URL", "http://localhost:8090")


def show(title: str, value) -> None:
    text = value if isinstance(value, str) else json.dumps(value, ensure_ascii=False)
    print(f"\n### {title}\n{text}")


async def main() -> None:
    # 서버를 자식 프로세스로 실행하고 stdio 로 연결한다. (호스트가 하는 일)
    params = StdioServerParameters(
        command=sys.executable, args=["server.py"], env={**os.environ, "MEMO_API_URL": API_URL}
    )
    async with Client(params) as client:
        tools = await client.list_tools()
        show("tools/list", [
            {"name": t.name, "readOnly": bool(t.annotations and t.annotations.read_only_hint)} for t in tools.tools
        ])
        show("resources/templates/list", [t.uri_template for t in (await client.list_resource_templates()).resource_templates])
        show("prompts/list", [p.name for p in (await client.list_prompts()).prompts])

        r = await client.call_tool("create_memo", {"title": "MCP 로 만든 메모", "content": "AI 가 도구로 저장했다", "tags": ["MCP", " AI "]})
        show("tools/call create_memo", r.structured_content or r.content[0].text)
        await client.call_tool("create_memo", {"title": "태그 없는 메모"})

        r = await client.call_tool("list_memos", {"tag": "mcp"})
        show("tools/call list_memos(tag=mcp)", r.structured_content or r.content[0].text)
        r = await client.call_tool("list_tags", {})
        show("tools/call list_tags", r.structured_content or r.content[0].text)

        memo_id = (await client.call_tool("list_memos", {"tag": "mcp"})).structured_content["result"][0]["id"]
        res = await client.read_resource(f"memo://{memo_id}")
        show(f"resources/read memo://{memo_id}", res.contents[0].text)
        p = await client.get_prompt("summarize_memos", {"tag": "mcp"})
        show("prompts/get summarize_memos", p.messages[0].content.text)

        # 잘못된 입력: 도구가 오류를 돌려주면 AI 는 그 메시지를 읽고 대응한다
        r = await client.call_tool("create_memo", {"title": "", "tags": ["x"]})
        show(f"tools/call create_memo(title='') isError={r.is_error}", r.content[0].text)
        r = await client.call_tool("get_memo", {"memo_id": 999999})
        show(f"tools/call get_memo(999999) isError={r.is_error}", r.content[0].text)


def wait_for_api() -> None:
    for _ in range(60):
        try:
            if httpx.get(f"{API_URL}/actuator/health", timeout=2).status_code == 200:
                return
        except httpx.HTTPError:
            pass
        time.sleep(2)
    raise SystemExit("메모 API 가 준비되지 않았다")


if __name__ == "__main__":
    wait_for_api()
    asyncio.run(main())
