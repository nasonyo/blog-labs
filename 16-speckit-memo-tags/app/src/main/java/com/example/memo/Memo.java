package com.example.memo;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import org.hibernate.annotations.SortNatural;

@Entity
@Table(name = "memo")
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // 태그는 메모에 붙는 값이다. 별도 엔티티 없이 memo_tag 테이블에 저장한다.
    // EAGER: open-in-view 가 꺼져 있고 엔티티를 그대로 응답으로 내보내므로 지연 로딩하면 직렬화 때 실패한다.
    // SortNatural: DB 정렬(collation)에 기대지 않고 자바에서 항상 이름순으로 유지한다. 새로 만든 값과 DB 에서 읽은 값의 순서가 같다.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "memo_tag", joinColumns = @JoinColumn(name = "memo_id"))
    @Column(name = "tag", length = 30, nullable = false)
    @SortNatural
    private SortedSet<String> tags = new TreeSet<>();

    protected Memo() {} // JPA 용 기본 생성자

    public Memo(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public SortedSet<String> getTags() { return tags; }

    public void setTags(Collection<String> tags) {
        this.tags = new TreeSet<>(tags);
    }
}
