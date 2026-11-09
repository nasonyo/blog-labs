"""두 에이전트가 함께 쓰는 도구. 샘플 로그 파일(data/)만 읽고, 아무것도 변경하지 않는다."""
import json
from pathlib import Path

from langchain_core.tools import tool

DATA = Path(__file__).resolve().parent.parent / "data"


def _read_lines(service: str) -> list[str]:
    path = DATA / "logs" / f"{service}.log"
    if not path.exists():
        return []
    return path.read_text(encoding="utf-8").splitlines()


def known_services() -> list[str]:
    return sorted(p.stem for p in (DATA / "logs").glob("*.log"))


@tool
def read_logs(service: str, last_n: int = 20) -> str:
    """서비스의 최근 로그를 읽는다. service 는 'memo-app' 또는 'web' 이다."""
    lines = _read_lines(service)
    if not lines:
        return f"'{service}' 로그가 없다. 사용 가능한 서비스: memo-app, web"
    return "\n".join(lines[-last_n:])


@tool
def count_errors(service: str) -> str:
    """서비스 로그에서 ERROR 와 WARN 의 개수를 센다. service 는 'memo-app' 또는 'web' 이다."""
    lines = _read_lines(service)
    if not lines:
        return f"'{service}' 로그가 없다. 사용 가능한 서비스: memo-app, web"
    errors = sum(1 for l in lines if " ERROR " in l)
    warns = sum(1 for l in lines if " WARN " in l)
    return json.dumps({"service": service, "error": errors, "warn": warns, "total_lines": len(lines)})


@tool
def get_deploy_history(service: str) -> str:
    """서비스의 최근 배포 이력을 조회한다. service 는 'memo-app' 또는 'web' 이다."""
    history = json.loads((DATA / "deploy_history.json").read_text(encoding="utf-8"))
    items = [h for h in history if h["service"] == service]
    return json.dumps(items, ensure_ascii=False) if items else f"'{service}' 배포 이력이 없다."


ALL_TOOLS = [read_logs, count_errors, get_deploy_history]
