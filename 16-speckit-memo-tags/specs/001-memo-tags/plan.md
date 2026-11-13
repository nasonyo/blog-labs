# Implementation Plan: 메모 태그

**Branch**: `001-memo-tags` | **Date**: 2026-11-13 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-memo-tags/spec.md`

## Summary

메모에 태그를 붙이고(P1), 태그로 목록을 거르고(P2), 사용 중인 태그와 개수를 조회한다(P3).
기존 Spring Boot + JPA 서비스에 **JPA 의 `@ElementCollection`** 으로 태그를 저장한다. 별도 `Tag` 엔티티와 새 인프라는
만들지 않는다. 기존 요청과 응답은 필드만 추가하는 방식(선택 필드)으로 확장해서 하위 호환을 유지한다.
입력 검증 오류는 문제가 된 필드를 알려주는 공통 오류 본문으로 돌려준다.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**: Spring Boot 3.3.5 (Web, Data JPA, Validation). 운영 의존성 추가 없음

**Storage**: PostgreSQL 16 (운영), H2 (테스트 전용). 태그는 메모와 1:N 값 컬렉션 테이블 `memo_tag` 에 저장

**Testing**: JUnit 5 + Spring Boot Test + MockMvc (테스트 범위 의존성 `spring-boot-starter-test`, `h2` 추가)

**Target Platform**: Docker 컨테이너 (Linux)

**Project Type**: web-service (REST API)

**Performance Goals**: 별도 목표 없음. 메모 수천 건 규모의 조회에서 체감 지연이 없는 정도

**Constraints**: 기존 엔드포인트 호환(Constitution II), 로컬에는 Docker 만 필요(Constitution V)

**Scale/Scope**: 메모 수천 건, 메모당 태그 최대 10개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판단 | 근거 |
|---|---|---|
| I. Test-First | PASS | tasks.md 가 각 사용자 스토리마다 "테스트 작성 → 실패 확인 → 구현" 순서를 강제한다 |
| II. Backward-Compatible API | PASS | 요청 `tags` 는 선택, 응답에 `tags` 필드 추가, 조회 파라미터 `tag` 는 선택. 기존 엔드포인트의 경로와 필드는 변경하지 않는다 |
| III. Simplicity | PASS | `Tag` 엔티티, 검색 엔진, 캐시를 만들지 않는다. 운영 의존성 추가 없음. 테스트 의존성 2개는 Complexity Tracking 에 기록 |
| IV. Clear Errors | PASS | 공통 오류 본문에 `errors[].field` 를 포함한다 (contracts/api.md) |
| V. Reproducible Environment | PASS | 테스트와 실행 모두 Docker 만으로 가능 (quickstart.md) |

**Post-design 재평가**: 변경 없음. 모두 PASS.

## Project Structure

### Documentation (this feature)

```text
specs/001-memo-tags/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── api.md
├── checklists/
│   └── requirements.md
└── tasks.md          # /speckit-tasks 가 생성
```

### Source Code (repository root)

기존 구조를 유지하고 파일을 추가/수정한다.

```text
app/
├── pom.xml                                          # 수정: 테스트 의존성 추가
└── src/
    ├── main/java/com/example/memo/
    │   ├── Memo.java                                # 수정: tags 필드
    │   ├── MemoRepository.java                      # 수정: 태그 조회/집계 쿼리
    │   ├── MemoController.java                      # 수정: 요청/응답/필터
    │   ├── TagNormalizer.java                       # 신규: 정규화와 검증
    │   ├── TagController.java                       # 신규: GET /api/tags
    │   └── ApiExceptionHandler.java                 # 신규: 공통 오류 본문
    └── test/
        ├── java/com/example/memo/
        │   ├── TagNormalizerTest.java               # 신규
        │   ├── MemoTagApiTest.java                  # 신규: P1, P2
        │   └── TagListApiTest.java                  # 신규: P3
        └── resources/application.yml                # 신규: H2 설정
```

**Structure Decision**: 단일 Spring Boot 서비스에 기능을 추가한다. 태그 규칙(정규화·검증)은 `TagNormalizer` 한 곳에
모아서 단위 테스트한다.

## Complexity Tracking

| 항목 | 필요한 이유 | 더 단순한 대안을 쓰지 않은 이유 |
|---|---|---|
| 테스트 의존성 `spring-boot-starter-test`, `h2` | Constitution I 이 자동화 테스트를 요구하는데 현재 프로젝트에는 테스트 기반이 없다 | DB 를 띄워야만 테스트되는 구조는 Constitution V(Docker 하나로 재현) 의 속도·간결성을 해친다. H2 는 헌법의 기술 제약에 테스트 전용으로 이미 허용되어 있다 |
