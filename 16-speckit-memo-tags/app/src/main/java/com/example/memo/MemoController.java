package com.example.memo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memos")
public class MemoController {

    private final MemoRepository repository;

    public MemoController(MemoRepository repository) {
        this.repository = repository;
    }

    public record MemoRequest(
            @NotBlank @Size(max = 100) String title,
            @Size(max = 1000) String content,
            List<String> tags) {}   // 선택. 생략하면 태그 없음

    @GetMapping
    public List<Memo> list(@RequestParam(required = false) String tag) {
        if (tag == null) {
            return repository.findAll();   // 필터가 없으면 이전과 똑같다
        }
        return repository.findByTag(TagNormalizer.normalizeOne(tag));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Memo> get(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Memo create(@Valid @RequestBody MemoRequest request) {
        Memo memo = new Memo(request.title(), request.content());
        memo.setTags(TagNormalizer.normalize(request.tags()));
        return repository.save(memo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
