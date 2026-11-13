package com.example.memo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 메서드를 직접 구현하지 않아도 save, findById, findAll, deleteById 등이 제공된다
public interface MemoRepository extends JpaRepository<Memo, Long> {

    // 정규화된 태그 하나가 붙은 메모. 태그 컬렉션은 즉시 로딩이라 메모의 태그 전체가 함께 읽힌다.
    @Query("select m from Memo m join m.tags t where t = :tag order by m.id")
    List<Memo> findByTag(@Param("tag") String tag);

    // 태그별 메모 수. 정렬은 DB 정렬 규칙(collation)에 기대지 않으려고 자바에서 한다.
    @Query("select t as name, count(m) as count from Memo m join m.tags t group by t")
    List<TagCount> countTags();

    interface TagCount {
        String getName();

        long getCount();
    }
}
