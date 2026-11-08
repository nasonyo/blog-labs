package com.example.memo;

import org.springframework.data.jpa.repository.JpaRepository;

// 메서드를 직접 구현하지 않아도 save, findById, findAll, deleteById 등이 제공된다
public interface MemoRepository extends JpaRepository<Memo, Long> {
}
