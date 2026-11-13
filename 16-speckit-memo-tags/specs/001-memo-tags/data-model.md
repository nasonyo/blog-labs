# Data Model: 메모 태그

## Memo (기존 + 변경)

| 필드 | 타입 | 비고 |
|---|---|---|
| id | bigint | 기존 |
| title | varchar(100) | 기존 |
| content | varchar(1000) | 기존 |
| created_at | timestamp | 기존 |
| **tags** | Set<String> | **신규.** 0~10개, 정규화된 값. 항상 **이름순 정렬** (`SortedSet`, `@SortNatural`). 즉시 로딩(EAGER) |

## memo_tag (신규 테이블, JPA 가 자동 생성)

| 컬럼 | 타입 | 제약 |
|---|---|---|
| memo_id | bigint | FK → memo.id, 메모 삭제 시 함께 삭제 |
| tag | varchar(30) | not null, (memo_id, tag) 중복 불가 |

## 검증 규칙 (TagNormalizer)

1. 각 태그의 앞뒤 공백을 제거한다.
2. 소문자로 바꾼다. (`Locale.ROOT`)
3. 결과가 비어 있으면 거부한다. (FR-008)
4. 길이가 30자를 넘으면 거부한다. (FR-008)
5. 중복을 제거한다. (FR-004)
6. 중복 제거 후 개수가 10개를 넘으면 거부한다. (FR-009)

## 상태 전이

없음. 태그는 메모 생성 시점에 정해지고 이후 수정되지 않는다(FR-011).
