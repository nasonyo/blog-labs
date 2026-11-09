import { Link, Route, Routes } from "react-router-dom";
import MemoList from "./pages/MemoList";
import MemoDetail from "./pages/MemoDetail";

export default function App() {
  return (
    <div className="container">
      <header>
        <Link to="/">
          <h1>Memo</h1>
        </Link>
      </header>
      <Routes>
        <Route path="/" element={<MemoList />} />
        <Route path="/memos/:id" element={<MemoDetail />} />
        <Route path="*" element={<p>페이지를 찾을 수 없습니다.</p>} />
      </Routes>
    </div>
  );
}
