import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { createMemo, deleteMemo, listMemos, Memo } from "../api";

export default function MemoList() {
  const [memos, setMemos] = useState<Memo[]>([]);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);

  const load = () =>
    listMemos()
      .then((data) => {
        setMemos(data);
        setError(null);
      })
      .catch((e: Error) => setError(`목록을 불러오지 못했습니다: ${e.message}`));

  useEffect(() => {
    load();
  }, []);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    try {
      await createMemo(title.trim(), content);
      setTitle("");
      setContent("");
      await load();
    } catch (err) {
      setError(`저장하지 못했습니다: ${(err as Error).message}`);
    }
  };

  const onDelete = async (id: number) => {
    try {
      await deleteMemo(id);
      await load();
    } catch (err) {
      setError(`삭제하지 못했습니다: ${(err as Error).message}`);
    }
  };

  return (
    <>
      <form onSubmit={onSubmit} className="card">
        <input
          placeholder="제목 (필수, 100자 이하)"
          value={title}
          maxLength={100}
          onChange={(e) => setTitle(e.target.value)}
        />
        <textarea
          placeholder="내용"
          value={content}
          maxLength={1000}
          onChange={(e) => setContent(e.target.value)}
        />
        <button type="submit">추가</button>
      </form>

      {error && <p className="error">{error}</p>}

      <ul className="list">
        {memos.map((m) => (
          <li key={m.id} className="card">
            <div>
              <Link to={`/memos/${m.id}`}>
                <strong>{m.title}</strong>
              </Link>
              <p className="muted">{new Date(m.createdAt).toLocaleString("ko-KR")}</p>
            </div>
            <button className="danger" onClick={() => onDelete(m.id)}>
              삭제
            </button>
          </li>
        ))}
        {memos.length === 0 && !error && <p className="muted">메모가 없습니다.</p>}
      </ul>
    </>
  );
}
