#!/usr/bin/env bash
# 간단한 부하 생성기: 생성/조회/없는 id 조회(404)/검증 실패(400)를 섞어서 보낸다
# 사용법: ./load.sh [초]   (기본 60초)
set -u
BASE=${BASE:-http://localhost:8090}
END=$((SECONDS + ${1:-60}))
i=0
while [ $SECONDS -lt $END ]; do
  i=$((i + 1))
  curl -s -o /dev/null -X POST "$BASE/api/memos" -H 'Content-Type: application/json' \
       -d "{\"title\":\"메모 $i\",\"content\":\"load test\"}"
  curl -s -o /dev/null "$BASE/api/memos"
  curl -s -o /dev/null "$BASE/api/memos/$((i % 50 + 1))"
  if [ $((i % 5)) -eq 0 ]; then
    curl -s -o /dev/null "$BASE/api/memos/999999"                                   # 404
    curl -s -o /dev/null -X POST "$BASE/api/memos" -H 'Content-Type: application/json' -d '{"title":""}'  # 400
  fi
  sleep 0.1
done
echo "done: $i iterations"
