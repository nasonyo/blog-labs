# Convergence Report: 메모 태그

`/speckit-converge` 단계의 결과다. 현재 코드를 spec, plan, tasks 와 비교해서 **아직 만들지 않은 일**을 tasks.md 에 추가한다.
(점검 방법: 테스트 27건 통과 확인 + 실행 중인 서비스에 경계 입력을 직접 호출)

## 만든 것 (요약)

FR-001~010 의 정상/거부 경로는 테스트와 실제 PostgreSQL 실행으로 확인했다. (FR-011 은 범위 밖)

## 남은 일

| ID | 심각도 | 내용 | 근거 |
|---|---|---|---|
| G1 | **Medium** | `tags` 가 배열이 아니라 문자열(`"tags":"abc"`)이면 400 이지만 **오류 본문에 `errors` 가 없다.** 문제가 된 입력을 알 수 없다 | SC-004, Constitution IV |
| G2 | Low | 깨진 JSON 도 마찬가지로 `errors` 가 없다 (기존부터 그랬던 동작이지만 Constitution IV 의 적용 대상이다) | Constitution IV |
| G3 | 정보 | `"tags":[1,2]` 같은 숫자 배열은 `["1","2"]` 로 변환되어 저장된다. 명세에 이 경우가 없다 | 결정 필요 — 작업으로 추가하지 않고 spec Assumptions 에 기록한다 |

## 추가한 작업

tasks.md 의 "Phase 7: Convergence" 참고 (T034~T037).
