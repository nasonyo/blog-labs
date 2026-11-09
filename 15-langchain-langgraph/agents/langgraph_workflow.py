"""LangGraph 로 만든 장애 대응 워크플로.

로그 수집 -> 심각도 분류(재시도 루프) -> (높음) 원인 분석 -> 사람 승인 -> 롤백 -> 보고서
                                   -> (낮음) 보고서

사용법: python -m agents.langgraph_workflow <service> <decision>
  service  : memo-app | web
  decision : approve | reject | ask   (승인 단계에서 사람의 결정. ask 는 터미널에서 직접 입력)
"""
import os
import sys
from typing import Literal, TypedDict

from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.types import Command, interrupt
from langchain_core.exceptions import OutputParserException
from pydantic import BaseModel, Field, ValidationError

from agents.llm import get_model
from agents.tools import get_deploy_history, known_services, read_logs

MAX_CLASSIFY_ATTEMPTS = 3


# ---------- 1) 상태: 모든 노드가 읽고 쓰는 공유 데이터 ----------
class IncidentState(TypedDict, total=False):
    service: str
    logs: str
    history: str
    severity: str          # "low" | "high" | "unknown"
    reason: str
    attempts: int
    analysis: str
    approved: bool
    action: str
    report: str


class Classification(BaseModel):
    severity: Literal["low", "high"] = Field(
        description="서비스 장애나 기동 실패가 있으면 high, 경고 수준이면 low")
    reason: str = Field(description="판단 근거 한 문장")


# ---------- 2) 노드: 상태를 받아서 바꿀 값만 돌려주는 함수 ----------
def fetch_logs(state: IncidentState) -> dict:
    svc = state["service"]
    # 입력 검증: 로그가 없는 서비스를 "문제 없음(low)"으로 분류하면 안 되므로 여기서 멈춘다
    if svc not in known_services():
        raise ValueError(f"알 수 없는 서비스: {svc!r} (사용 가능: {', '.join(known_services())})")
    return {
        "logs": read_logs.invoke({"service": svc, "last_n": 30}),
        "history": get_deploy_history.invoke({"service": svc}),
        "attempts": 0,
    }


def classify(state: IncidentState) -> dict:
    attempts = state.get("attempts", 0) + 1
    # [테스트용] 재시도 루프를 보여주려고, 앞의 N 번은 일부러 실패한 것으로 처리한다 (기본 0 = 사용 안 함)
    if attempts <= int(os.getenv("SIMULATE_CLASSIFY_FAILURES", "0")):
        print(f"    (분류 실패 {attempts}회: 시뮬레이션)")
        return {"severity": "unknown", "reason": "", "attempts": attempts}
    llm = get_model().with_structured_output(Classification)
    try:
        result = llm.invoke(
            "다음 로그를 보고 심각도를 분류해라. 서비스가 기동하지 못하거나 반복적으로 죽으면 high, "
            "단순 경고나 느린 요청 정도면 low 다. reason 은 반드시 한국어로 쓴다.\n\n" + state["logs"])
        if result is None:
            raise OutputParserException("구조화된 출력이 비어 있다")
        return {"severity": result.severity, "reason": result.reason, "attempts": attempts}
    except (OutputParserException, ValidationError) as e:
        # 모델이 형식에 맞는 출력을 못 만든 경우만 재시도 대상이다.
        # 네트워크/인증 오류 같은 인프라 문제는 잡지 않고 그대로 올려서 바로 드러나게 한다.
        print(f"    (분류 실패 {attempts}회: {type(e).__name__})")
        return {"severity": "unknown", "reason": "", "attempts": attempts}


def analyze(state: IncidentState) -> dict:
    msg = get_model().invoke(
        "너는 SRE 다. 아래 로그와 배포 이력을 보고 장애 원인을 2~3문장으로 설명하고, "
        "되돌릴(롤백) 필요가 있는지 한 문장으로 제안해라. 한국어로 답한다.\n\n"
        f"[로그]\n{state['logs']}\n\n[배포 이력]\n{state['history']}")
    return {"analysis": msg.text}


def approval(state: IncidentState) -> dict:
    # interrupt: 여기서 그래프 실행이 멈추고, 상태가 체크포인트에 저장된다.
    # 사람이 Command(resume=...) 로 값을 넣어 주면 이 줄이 그 값을 돌려주며 이어서 실행된다.
    decision = interrupt({
        "question": f"{state['service']} 을(를) 직전 버전으로 롤백할까요?",
        "analysis": state["analysis"],
    })
    return {"approved": bool(decision)}


def rollback(state: IncidentState) -> dict:
    # 실습용 시뮬레이션: 실제 명령은 실행하지 않는다.
    cmd = f"kubectl -n memo rollout undo deployment/{state['service']}"
    return {"action": f"[시뮬레이션] 실행했다고 가정: {cmd}"}


def report(state: IncidentState) -> dict:
    if state.get("approved") is True:
        done = state["action"]
    elif state.get("approved") is False:
        done = "사람이 롤백을 거부했다. 롤백은 수행하지 않았다."
    else:
        done = "조치 없음 (심각도가 낮아 승인 절차를 거치지 않았다)."
    msg = get_model().invoke(
        "아래 정보로 장애 보고서를 한국어로 4줄 이내로 작성해라. 없는 사실은 쓰지 마라.\n"
        f"서비스: {state['service']}\n심각도: {state['severity']} ({state.get('reason', '')})\n"
        f"원인 분석: {state.get('analysis', '(분석 생략)')}\n조치: {done}")
    return {"report": msg.text}


# ---------- 3) 분기 함수: 다음에 갈 노드 이름을 돌려준다 ----------
def after_classify(state: IncidentState) -> Literal["classify", "analyze", "report"]:
    sev = state["severity"]
    if sev == "unknown":
        # 분류 실패: 재시도 루프. 횟수를 넘기면 안전하게 high 로 취급한다.
        return "classify" if state["attempts"] < MAX_CLASSIFY_ATTEMPTS else "analyze"
    return "analyze" if sev == "high" else "report"


def after_approval(state: IncidentState) -> Literal["rollback", "report"]:
    return "rollback" if state["approved"] else "report"


# ---------- 4) 그래프 조립 ----------
def build_graph():
    g = StateGraph(IncidentState)
    g.add_node("fetch_logs", fetch_logs)
    g.add_node("classify", classify)
    g.add_node("analyze", analyze)
    g.add_node("approval", approval)
    g.add_node("rollback", rollback)
    g.add_node("report", report)

    g.add_edge(START, "fetch_logs")
    g.add_edge("fetch_logs", "classify")
    g.add_conditional_edges("classify", after_classify)
    g.add_edge("analyze", "approval")
    g.add_conditional_edges("approval", after_approval)
    g.add_edge("rollback", "report")
    g.add_edge("report", END)
    # 체크포인터가 있어야 interrupt 로 멈췄다가 이어서 실행할 수 있다
    return g.compile(checkpointer=InMemorySaver())


# ---------- 5) 실행 ----------
def short(text: str, n: int = 110) -> str:
    text = str(text).replace("\n", " ")
    return text if len(text) <= n else text[:n] + "..."


def run_stream(graph, payload, config) -> dict | None:
    """노드 단위로 진행 상황을 출력한다. interrupt 가 걸리면 그 내용을 돌려준다."""
    for chunk in graph.stream(payload, config, stream_mode="updates"):
        for node, update in chunk.items():
            if node == "__interrupt__":
                return update[0].value
            keys = {k: short(v) for k, v in update.items() if k not in ("logs", "history")}
            print(f"[노드 실행] {node:<10} -> {keys}")
    return None


def main(service: str, decision: str) -> None:
    graph = build_graph()
    config = {"configurable": {"thread_id": f"incident-{service}"}}
    print(f"[시작] service={service}\n")

    try:
        pending = run_stream(graph, {"service": service}, config)
    except ValueError as e:
        print(f"\n[오류] {e}")
        sys.exit(2)
    if pending is None:
        print("\n[종료] 승인 절차 없이 끝났다.")
    else:
        print(f"\n[중단] 사람의 승인을 기다린다. 다음에 실행될 노드: {graph.get_state(config).next}")
        print(f"       질문: {pending['question']}")
        print(f"       분석: {short(pending['analysis'], 200)}")
        if decision == "ask":
            answer = input("       롤백할까요? (y/n): ").strip().lower() == "y"
        else:
            answer = decision == "approve"
        print(f"       사람의 결정: {'승인' if answer else '거부'}\n")
        run_stream(graph, Command(resume=answer), config)

    final = graph.get_state(config).values
    print("\n[최종 보고서]\n" + final["report"])


if __name__ == "__main__":
    svc = sys.argv[1] if len(sys.argv) > 1 else "memo-app"
    dec = sys.argv[2] if len(sys.argv) > 2 else "approve"
    if "--graph" in sys.argv:
        print(build_graph().get_graph().draw_mermaid())
    else:
        main(svc, dec)
