# --- test 스테이지: 테스트가 실패하면 이미지 빌드가 여기서 멈춘다 ---
FROM python:3.12-slim AS test
WORKDIR /app
COPY requirements.txt requirements-dev.txt ./
RUN pip install --no-cache-dir -r requirements.txt -r requirements-dev.txt
COPY . .
RUN python -m pytest -q

# --- runtime 스테이지: 테스트 도구 없이 실행에 필요한 것만 ---
FROM python:3.12-slim AS runtime
WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .
EXPOSE 8000
CMD ["gunicorn", "-b", "0.0.0.0:8000", "app:app"]
