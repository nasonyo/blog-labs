FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY agents ./agents
COPY data ./data
# 실행할 스크립트는 compose 의 command 로 정한다
