# API Contract: 메모 태그

기존 엔드포인트는 경로와 기존 필드를 바꾸지 않는다. 아래는 **추가되거나 확장되는 부분**만 적는다.

## POST /api/memos (확장)

요청 본문에 선택 필드 `tags` 를 추가한다.

```json
{ "title": "주간 회의", "content": "안건 정리", "tags": ["회의", "Weekly"] }
```

- `tags` 는 생략하거나 `null`, `[]` 이어도 된다. (기존 요청 호환)
- 성공: `201`. 응답 본문의 `tags` 는 정규화된 값이다.

```json
{ "id": 1, "title": "주간 회의", "content": "안건 정리", "createdAt": "...", "tags": ["회의", "weekly"] }
```

- 검증 실패: `400`, 오류 본문은 아래 "공통 오류 본문" 참고.

## GET /api/memos, GET /api/memos/{id} (응답 확장)

- 응답의 각 메모에 `tags` (문자열 배열, 없으면 `[]`) 가 포함되며, **이름순으로 정렬**된다.

## GET /api/memos?tag={name} (신규 파라미터)

- `tag` 를 정규화한 값이 붙은 메모만 돌려준다. 생략하면 전체 목록이다. 일치하는 메모가 없으면 `200` 과 `[]`.
- `tag` 가 공백뿐이거나 30자를 넘으면 `400`.

## GET /api/tags (신규)

```json
[ { "name": "weekly", "count": 1 }, { "name": "회의", "count": 2 } ]
```

- 이름순 정렬. 메모가 없으면 `[]`.

## 공통 오류 본문 (400)

기존 필드(`timestamp`, `status`, `error`, `path`)는 유지하고 `errors` 를 추가한다.

```json
{
  "timestamp": "...", "status": 400, "error": "Bad Request", "path": "/api/memos",
  "errors": [ { "field": "tags", "message": "태그는 최대 10개까지 붙일 수 있습니다" } ]
}
```

| 상황 | field | 메시지에 포함할 내용 |
|---|---|---|
| 빈/공백 태그 | `tags[i]` | 비어 있을 수 없다 |
| 31자 이상 태그 | `tags[i]` | 최대 30자 |
| 태그 11개 이상 | `tags` | 최대 10개 |
| 제목이 비어 있음 (기존 검증) | `title` | 비어 있을 수 없다 |
