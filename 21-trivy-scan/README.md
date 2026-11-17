# 21-trivy-scan

Trivy 로 Docker 이미지의 취약점(HIGH, CRITICAL)을 스캔하고, 줄이는 방법과 CI 게이트까지 확인한 실습.

- 환경: Docker 29.9.2, aquasec/trivy:0.58.1 (컨테이너로 실행), 2026-11-17 한 번 실행
- 취약점 DB 는 매일 바뀌므로 같은 명령도 날짜에 따라 숫자가 달라진다.
- 스캔 범위는 OS 패키지 취약점(`--scanners vuln`)뿐이다. 시크릿, 설정 오류, 애플리케이션 의존성은 보지 않았다.

## 실행
```bash
T() { docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v trivy-cache:/root/.cache aquasec/trivy:0.58.1 "$@"; }
T image --scanners vuln --severity HIGH,CRITICAL nginx:1.18
docker build -t nginx-patched:1 .      # Dockerfile: apk upgrade 한 줄 추가
T image --scanners vuln --severity CRITICAL --exit-code 1 nginx-patched:1; echo $?
```

## 로그
- `logs/01-scan.txt`: 오래된 이미지와 1.27-alpine 스캔
- `logs/02-detail.txt`: 수정 버전이 있는 취약점 수, 많이 나온 패키지
- `logs/03-fix.txt`: apk upgrade 후, 새로 받은 이미지, CI 게이트 종료 코드
