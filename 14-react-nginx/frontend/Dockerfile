# --- build 스테이지: Node 로 정적 파일(HTML/JS/CSS)을 만든다 ---
FROM node:20.18-alpine AS build
WORKDIR /app
# 의존성 파일만 먼저 복사하면 소스만 바뀔 때 npm ci 레이어를 캐시로 재사용한다
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# API 주소를 바꾸고 싶을 때: docker build --build-arg VITE_API_BASE=http://localhost:8090 ...
ARG VITE_API_BASE=""
ENV VITE_API_BASE=${VITE_API_BASE}
RUN npm run build

# --- runtime 스테이지: 빌드 결과물만 Nginx 에 담는다 (Node 는 최종 이미지에 없다) ---
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
