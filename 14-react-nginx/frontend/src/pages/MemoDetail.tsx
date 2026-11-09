import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getMemo, Memo } from "../api";

export default function MemoDetail() {
  const { id } = useParams();
  const [memo, setMemo] = useState<Memo | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    getMemo(id)
      .then(setMemo)
      .catch((e: Error) => setError(`메모를 불러오지 못했습니다: ${e.message}`));
  }, [id]);

  return (
    <div className="card column">
      <Link to="/">← 목록</Link>
      {error && <p className="error">{error}</p>}
      {memo && (
        <>
          <h2>{memo.title}</h2>
          <p className="muted">{new Date(memo.createdAt).toLocaleString("ko-KR")}</p>
          <p style={{ whiteSpace: "pre-wrap" }}>{memo.content}</p>
        </>
      )}
    </div>
  );
}
