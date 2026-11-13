# Specification Analysis Report: 메모 태그

`/speckit-analyze` 단계의 결과다. 이 단계는 파일을 수정하지 않는다. (이 파일은 보고서를 보존하려고 따로 저장한 것이다.)

## Findings

| ID | 분류 | 심각도 | 위치 | 요약 | 권장 조치 |
|---|---|---|---|---|---|
| A1 | 불일치 / 미명세 | **Medium** | data-model.md, contracts/api.md, spec FR-002 | 데이터 모델은 태그를 "삽입 순서를 유지하는 Set" 으로, 계약의 예시 응답은 이름순처럼 보이게 적었다. JPA 가 DB 에서 다시 읽은 컬렉션은 삽입 순서를 보장하지 않으므로 **응답의 `tags` 순서가 실행마다 달라질 수 있다.** 순서 요구와 테스트가 없다 | 응답의 `tags` 를 **이름순**으로 정의하고(FR-002), 정렬 테스트 작업을 추가한다 |
| A2 | 추적성 | Low | tasks.md | FR-001, FR-002, FR-007, SC-001 이 어떤 작업에서도 ID 로 언급되지 않는다. 내용은 T008/T014/T025 가 간접적으로 다루지만 추적할 수 없다 | 해당 작업에 FR/SC ID 를 적는다 |
| A3 | 커버리지 | Low | contracts/api.md, spec FR-008 | `GET /api/memos?tag=` 의 잘못된 `tag` 값에 400 을 돌려준다고 계약에 있고 T020 이 테스트하지만, 명세(FR)에는 이 요구가 없다 | FR-008 에 "조회용 태그 값도 같은 규칙" 을 명시한다 |
| A4 | 정보 | Info | FR-011 | 태그 수정은 의도적으로 범위 밖이라 작업이 없다 | 조치 없음 |

## Coverage Summary

| 항목 | 값 |
|---|---|
| 기능 요구(FR) | 11 (이 중 1개는 범위 밖 선언) |
| 성공 기준(SC) | 4 |
| 작업 | 33 |
| 작업이 하나도 없는 FR | 0 (범위 밖 FR-011 제외) |
| ID 로 직접 추적되는 FR | 7 / 10 (FR-001, 002, 007 은 간접) |
| Critical 이슈 | 0 |

## Constitution Alignment

| 원칙 | 결과 |
|---|---|
| I. Test-First | 충족. 모든 스토리에 "테스트 작성 → 실패 확인 → 구현" 작업이 있다 |
| II. Backward-Compatible API | 충족. 기준선 테스트(T003)와 오류 본문 테스트(T006)가 호환을 고정한다 |
| III. Simplicity | 충족. 운영 의존성 추가 없음 |
| IV. Clear Errors | 충족 (T006, T011) |
| V. Reproducible Environment | 충족 (quickstart.md) |

## Next Actions

- A1 은 구현 전에 해결하는 것을 권장한다 (순서가 계약이 되기 때문이다).
- A2, A3 은 문서 보강이라 구현과 병행해도 된다.
