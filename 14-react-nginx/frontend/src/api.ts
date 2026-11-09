// 빌드할 때 VITE_API_BASE 로 API 주소를 바꿀 수 있다.
// 기본값(빈 문자열)은 "같은 주소의 /api" 이고, 운영에서는 Nginx 가 이것을 백엔드로 넘긴다.
const BASE = import.meta.env.VITE_API_BASE ?? "";

export interface Memo {
  id: number;
  title: string;
  content: string | null;
  createdAt: string;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, init);
  if (!res.ok) {
    throw new Error(`${res.status} ${res.statusText}`);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

export const listMemos = () => request<Memo[]>("/api/memos");
export const getMemo = (id: string) => request<Memo>(`/api/memos/${id}`);
export const createMemo = (title: string, content: string) =>
  request<Memo>("/api/memos", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title, content }),
  });
export const deleteMemo = (id: number) =>
  request<void>(`/api/memos/${id}`, { method: "DELETE" });
