FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .
# 빌드할 때 버전을 넣는다: docker build --build-arg APP_VERSION=v2 ...
ARG APP_VERSION=v1
ENV APP_VERSION=${APP_VERSION}
EXPOSE 8000
CMD ["gunicorn", "-b", "0.0.0.0:8000", "app:app"]
