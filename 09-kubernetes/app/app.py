import os
import socket

from flask import Flask, jsonify

app = Flask(__name__)


@app.get("/")
def index():
    # hostname 은 컨테이너(Pod) 이름이다. 어느 Pod 가 응답했는지 확인하는 용도
    return jsonify(
        message="Hello from demo-app",
        version=os.getenv("APP_VERSION", "unknown"),
        pod=socket.gethostname(),
    )


@app.get("/health")
def health():
    return jsonify(status="ok")
