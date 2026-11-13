# Research: 메모 태그

## R1. 태그를 어떻게 저장하는가

- **Decision**: `Memo` 에 `@ElementCollection Set<String> tags` 를 두고 `memo_tag(memo_id, tag)` 테이블에 저장한다.
- **Rationale**: 태그는 독립 관리 대상이 아니라 메모에 붙는 값이다(spec Key Entities). 별도 엔티티와 조인 테이블을
  만들 이유가 없고, `ddl-auto: update` 로 새 테이블이 자동 생성되어 기존 데이터에 영향이 없다(spec Assumptions).
- **Alternatives considered**:
  - `Tag` 엔티티 + `@ManyToMany`: 태그 이름 변경, 태그 메타데이터가 필요할 때 적합하지만 지금은 과하다(Constitution III).
  - 메모 행의 문자열 컬럼에 쉼표로 이어 저장: 태그 필터와 집계가 어렵고 이스케이프 문제가 생긴다.

- **구현 중 정정 (T015)**: 처음에는 기본 설정(지연 로딩, `@OrderBy`)으로 구현했다. 테스트가 두 가지를 잡아냈다.
  1. `open-in-view: false` 인 서비스가 엔티티를 그대로 응답으로 내보내서, 지연 로딩된 태그를 직렬화할 때 `LazyInitializationException` 이
     발생했고 **기존 목록 조회(기준선 테스트)까지 500** 이 되었다. → `fetch = EAGER`. 태그는 메모당 최대 10개라 비용이 작다.
  2. `@OrderBy` 는 DB 에서 읽을 때만 적용되어, 방금 만든 메모의 응답과 순서가 달랐고 DB 정렬 규칙(collation)에도 의존한다.
     → `@SortNatural` + `SortedSet`(TreeSet). 자바에서 항상 같은 순서가 된다.

## R2. 중복 제거와 순서

- **Decision**: 메모 안에서는 `Set`(삽입 순서 유지)으로 중복을 제거한다.
- **Rationale**: FR-004. 정규화(소문자, 공백 제거) 이후에 중복을 판단해야 `"회의"`, `" 회의 "` 가 하나가 된다.

## R3. 정규화와 검증 규칙의 위치

- **Decision**: `TagNormalizer` 클래스에 `normalize(List<String>)` 를 두고, 검증 실패 시 `InvalidTagException` 을 던진다.
- **Rationale**: 규칙(공백 제거, 소문자, 1~30자, 최대 10개)이 FR-008~010 에 걸쳐 있어서 한 곳에서 단위 테스트하는 것이
  컨트롤러 테스트보다 빠르고 명확하다.
- **Alternatives considered**: Bean Validation 어노테이션: 정규화(값 변경)를 표현하기 어렵다.

## R4. 태그 필터 조회

- **Decision**: `GET /api/memos?tag={name}`. 서버는 `tag` 도 같은 규칙으로 정규화한 뒤 JPQL 로 조회한다.
- **Rationale**: FR-005, FR-010. 필터 입력도 정규화해야 `Meeting` 으로 조회해도 `meeting` 메모가 나온다.
  존재하지 않는 태그는 빈 목록(FR-006).

## R5. 태그 집계

- **Decision**: `GET /api/tags` → `[{ "name": "...", "count": n }]`, JPQL `group by` 로 집계하고 이름순 정렬한다.
- **Rationale**: FR-007. 메모가 삭제되면 `memo_tag` 행도 함께 삭제되므로(JPA 값 컬렉션) 집계에서 자연스럽게 사라진다
  (spec Edge Cases 의 마지막 항목).

## R6. 오류 본문

- **Decision**: `@RestControllerAdvice` 에서 `{timestamp, status, error, path, errors:[{field, message}]}` 로 응답한다.
  기존 기본 오류 본문의 필드(`timestamp`, `status`, `error`, `path`)는 유지하고 `errors` 만 추가한다.
- **Rationale**: Constitution IV(필드 명시) 와 II(기존 필드 유지)를 동시에 만족한다. 지금은 `title` 이 비었을 때도
  어느 필드가 문제인지 응답에 없는데, 이 변경으로 함께 개선된다.

## R7. 테스트 환경

- **Decision**: `@SpringBootTest` + `MockMvc` + H2(`MODE=PostgreSQL`). 테스트는 `maven` 이미지로 `mvn test` 를 실행한다.
- **Rationale**: Constitution V. 운영 DB 와 SQL 방언 차이가 있을 수 있으므로, 구현 후 quickstart 에서 실제 PostgreSQL
  로도 한 번 검증한다.
