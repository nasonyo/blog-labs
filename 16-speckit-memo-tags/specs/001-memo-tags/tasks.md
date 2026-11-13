---

description: "Task list for 메모 태그"
---

# Tasks: 메모 태그

**Input**: Design documents from `/specs/001-memo-tags/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md

**Tests**: Constitution I(Test-First) 에 따라 **모든 사용자 스토리에 테스트 작업이 필수**이고, 구현 작업보다 먼저 와야 한다.
테스트는 구현 전에 실행해서 **기대한 이유로 실패하는 것**을 확인한 뒤에 구현한다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 다른 파일을 다루고 서로 의존하지 않아서 병렬로 해도 되는 작업
- **[Story]**: 어떤 사용자 스토리의 작업인지 (US1, US2, US3)

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 `app/pom.xml` 에 테스트 범위 의존성 `spring-boot-starter-test`, `h2` 를 추가한다
- [x] T002 [P] `app/src/test/resources/application.yml` 에 H2(`MODE=PostgreSQL`) 테스트 설정을 만든다
- [x] T003 기존 동작의 **기준선 테스트**를 `app/src/test/java/com/example/memo/MemoBaselineTest.java` 에 작성한다
      (태그 없는 메모 생성 201, 목록 조회, 단건 404) — 이후 모든 단계에서 계속 통과해야 한다 (SC-002)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 스토리가 공유하는 태그 규칙과 오류 본문

- [x] T004 [P] `TagNormalizerTest.java` 를 작성한다: 공백 제거, 소문자, 중복 제거, 빈 값/공백 거부, 31자 거부, 11개 거부 (FR-004, 008, 009, 010)
- [x] T005 T004 가 **실패하는 것을 확인**한 뒤 `TagNormalizer.java`, `InvalidTagException.java` 를 구현한다
- [x] T006 [P] `ApiErrorBodyTest.java` 를 작성한다: 400 응답에 `errors[].field` 가 있고 기존 필드(`timestamp`, `status`, `error`, `path`)도 있다 (Constitution II, IV)
- [x] T007 T006 이 실패하는 것을 확인한 뒤 `ApiExceptionHandler.java` 를 구현한다

**Checkpoint**: 정규화 규칙과 오류 본문이 단위/슬라이스 테스트로 고정되었다.

---

## Phase 3: User Story 1 - 메모를 만들 때 태그를 붙인다 (Priority: P1) 🎯 MVP

**Goal**: 태그를 붙여 메모를 만들고 조회 시 태그가 돌아온다.

**Independent Test**: 태그 2개로 메모를 만들고 같은 메모를 조회하면 두 태그가 그대로 돌아온다.

### Tests for User Story 1 (먼저 작성하고 실패를 확인한다)

- [x] T008 [US1] `MemoTagApiTest.java` 에 작성한다: 태그 포함 생성 시 응답과 단건 조회에 태그 포함 (Acceptance 1, FR-001, FR-002, SC-001)
- [x] T009 [US1] 같은 파일에: `tags` 없는 기존 요청은 201 이고 `tags` 는 `[]` (Acceptance 2, FR-003)
- [x] T010 [US1] 같은 파일에: 중복/대소문자/공백이 정규화되어 한 번만 저장 (Acceptance 3, FR-004, FR-010)
- [x] T010a [US1] 같은 파일에: 응답의 `tags` 가 이름순으로 정렬됨 (FR-002) — analyze 결과 A1 로 추가
- [x] T011 [US1] 같은 파일에: 빈 태그, 31자 태그, 11개 태그는 400 이고 `errors[].field` 가 문제 입력을 가리킴 (FR-008, FR-009, SC-004)

### Implementation for User Story 1

- [x] T012 [US1] T008~T011, T010a 가 **실패하는 것을 확인**한다
- [x] T013 [US1] `Memo.java` 에 `tags`(`@ElementCollection(fetch = EAGER)`, `@SortNatural`, `memo_tag`) 와 접근자를 추가한다 (FR-002)
- [x] T014 [US1] `MemoController.java` 의 `MemoRequest` 에 선택 필드 `tags` 를 추가하고 `TagNormalizer` 로 정규화해서 저장한다 (FR-001)
- [x] T015 [US1] T008~T011, T010a 와 T003 이 **모두 통과**하는 것을 확인한다

**Checkpoint**: P1 만으로 배포 가능한 MVP.

---

## Phase 4: User Story 2 - 태그로 목록을 걸러 본다 (Priority: P2)

**Goal**: `GET /api/memos?tag=` 로 태그가 붙은 메모만 조회한다.

**Independent Test**: 태그가 다른 메모 3개를 만들고 한 태그로 필터하면 해당 메모만 나온다.

### Tests for User Story 2

- [x] T016 [US2] `MemoTagApiTest.java` 에 추가한다: `tag=회의` 로 조회하면 회의 메모 2개만 (Acceptance 1, SC-003)
- [x] T017 [US2] 같은 파일에: `tag` 를 생략하면 전체 목록 (Acceptance 2, FR-005)
- [x] T018 [US2] 같은 파일에: 없는 태그는 200 과 `[]` (Acceptance 3, FR-006)
- [x] T019 [US2] 같은 파일에: `tag=Meeting`(대소문자/공백 다름)으로 `meeting` 메모가 조회됨 (FR-010)
- [x] T020 [US2] 같은 파일에: 공백뿐인 `tag` 는 400 (contracts/api.md)

### Implementation for User Story 2

- [x] T021 [US2] T016~T020 이 **실패하는 것을 확인**한다
- [x] T022 [US2] `MemoRepository.java` 에 태그로 조회하는 JPQL 을 추가한다
- [x] T023 [US2] `MemoController.java` 의 목록 조회에 선택 파라미터 `tag` 를 추가한다 (정규화 후 조회)
- [x] T024 [US2] T016~T020 과 이전 테스트가 **모두 통과**하는 것을 확인한다

---

## Phase 5: User Story 3 - 사용 중인 태그 목록을 본다 (Priority: P3)

**Goal**: `GET /api/tags` 로 태그와 메모 수를 본다.

**Independent Test**: 태그가 다른 메모를 만들고 태그 목록의 이름과 개수가 맞다.

### Tests for User Story 3

- [x] T025 [US3] `TagListApiTest.java` 에 작성한다: 태그별 개수와 이름순 정렬 (Acceptance 1, FR-007)
- [x] T026 [US3] 같은 파일에: 메모가 없으면 `[]` (Acceptance 2)
- [x] T027 [US3] 같은 파일에: 메모를 삭제하면 그 메모에만 있던 태그가 목록에서 사라짐 (Edge Cases)

### Implementation for User Story 3

- [x] T028 [US3] T025~T027 이 **실패하는 것을 확인**한다
- [x] T029 [US3] `MemoRepository.java` 에 집계 JPQL 을, `TagController.java` 에 `GET /api/tags` 를 구현한다
- [x] T030 [US3] T025~T027 과 이전 테스트가 **모두 통과**하는 것을 확인한다

---

## Phase 6: Polish & Cross-Cutting Concerns

- [x] T031 `docker compose up -d --build` 로 실제 PostgreSQL 에서 `quickstart.md` 의 시나리오를 모두 실행하고 기대 결과와 비교한다
- [x] T032 전체 테스트를 다시 실행하고, 기준선 테스트(T003)가 통과하는지 확인한다 (SC-002)
- [x] T033 `README` 에 API 변경(추가된 필드와 엔드포인트)을 기록한다

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → Phase 3(US1) → Phase 4(US2) → Phase 5(US3) → Phase 6
- US2 는 US1 의 태그 저장에, US3 는 US1 의 데이터에 의존한다. 우선순위(P1→P2→P3) 순서로 진행한다.
- 병렬 가능: T002, T004, T006 (서로 다른 파일)

## Implementation Strategy

1. **MVP**: Phase 1~3 (US1)까지 끝내고 중간 점검한다.
2. US2, US3 를 순서대로 추가한다. 각 단계 끝에서 이전 단계 테스트가 모두 통과해야 한다.
3. 각 작업은 별도 커밋으로 남기고 메시지에 작업 ID 를 쓴다.

---

## Phase 7: Convergence (added by `/speckit-converge`)

**Purpose**: 구현 점검에서 발견된 남은 일. 같은 규칙(Test-First)으로 처리한다. 자세한 근거는 converge-report.md.

- [x] T034 [US1] `MemoTagApiTest.java` 에 작성한다: `tags` 가 문자열이면 400 이고 `errors[0].field` 가 `tags` (SC-004, Constitution IV) — G1
- [x] T035 [US1] `MalformedBodyTest.java` 에 작성한다: 깨진 JSON 은 400 이고 `errors[0].field` 가 `body` — G2
- [x] T036 T034, T035 가 **실패하는 것을 확인**한 뒤 `ApiExceptionHandler.java` 에 `HttpMessageNotReadableException` 처리를 추가한다
- [x] T037 전체 테스트와 `quickstart.md` 의 오류 시나리오를 다시 실행한다

**Note (G3)**: `"tags":[1,2]` 는 문자열로 변환되어 저장된다. 명세에 없는 경우라서 작업을 만들지 않고 spec Assumptions 에 동작을 기록한다.
