package com.example.memo;

import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    public record TagSummary(String name, long count) {}

    private final MemoRepository repository;

    public TagController(MemoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<TagSummary> list() {
        return repository.countTags().stream()
                .map(t -> new TagSummary(t.getName(), t.getCount()))
                .sorted(Comparator.comparing(TagSummary::name))
                .toList();
    }
}
