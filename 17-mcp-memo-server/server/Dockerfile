FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY server.py check.py ./
# MCP 서버는 클라이언트(Claude Code 등)가 이 프로세스를 직접 실행하고 표준 입출력(stdio)으로 대화한다
ENTRYPOINT ["python", "server.py"]
