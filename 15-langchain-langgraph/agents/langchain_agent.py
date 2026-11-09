"""LangChain 으로 만든 에이전트: LLM 이 도구를 골라 쓰면서 질문에 답한다."""
import sys

from langchain.agents import create_agent

from agents.llm import get_model
from agents.tools import ALL_TOOLS

SYSTEM_PROMPT = (
    "너는 운영 로그를 분석하는 도우미다. 한국어로 간결하게 답한다.\n"
    "규칙:\n"
    "1. 질문에 답하는 데 필요한 도구(로그 개수, 로그 내용, 배포 이력)를 모두 호출한 뒤에 답한다.\n"
    "2. '확인하겠습니다' 같은 예고를 하지 말고, 필요하면 바로 도구를 호출한다.\n"
    "3. 도구 결과에 있는 내용만 근거로 답하고, 없는 내용은 모른다고 말한다."
)

QUESTION = "memo-app 서비스에 에러가 몇 건 있고, 원인이 뭐야? 최근 배포와 관련이 있는지도 알려줘."


def main(question: str) -> None:
    agent = create_agent(get_model(), tools=ALL_TOOLS, system_prompt=SYSTEM_PROMPT)
    print(f"[agent 타입] {type(agent).__module__}.{type(agent).__name__}\n")
    print(f"[질문] {question}\n")

    # stream_mode="values": 단계마다 메시지 전체가 나온다. 새로 추가된 메시지만 출력한다.
    seen = 0
    for state in agent.stream({"messages": [{"role": "user", "content": question}]}, stream_mode="values"):
        messages = state["messages"]
        for m in messages[seen:]:
            if m.type == "ai" and m.tool_calls:
                for c in m.tool_calls:
                    print(f"[LLM -> 도구 호출] {c['name']}({c['args']})")
            elif m.type == "tool":
                print(f"[도구 결과: {m.name}]\n{m.content}\n")
            elif m.type == "ai":
                print(f"[최종 답변]\n{m.text}")   # Gemini 는 content 가 블록 목록이라 .text 를 쓴다
        seen = len(messages)


if __name__ == "__main__":
    main(" ".join(sys.argv[1:]) or QUESTION)
