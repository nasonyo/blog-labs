package com.example.memo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class MemoTagApiTest {

    @Autowired MockMvc mvc;
    @Autowired MemoRepository repository;
    @Autowired ObjectMapper json;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private ResultActions create(String title, List<String> tags) throws Exception {
        String body = tags == null
                ? json.writeValueAsString(java.util.Map.of("title", title))
                : json.writeValueAsString(java.util.Map.of("title", title, "tags", tags));
        return mvc.perform(post("/api/memos").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createAndGetId(String title, List<String> tags) throws Exception {
        String res = create(title, tags).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    // ---------------- User Story 1 ----------------

    @Test // T008: Acceptance 1, FR-001, FR-002, SC-001
    void createdTagsAreReturnedOnCreateAndOnLookup() throws Exception {
        long id = createAndGetId("주간 회의", List.of("회의", "Weekly"));
        create("다른 메모", List.of("회의", "Weekly")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags[0]").value("weekly"))
                .andExpect(jsonPath("$.tags[1]").value("회의"));
        mvc.perform(get("/api/memos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(2));
    }

    @Test // T009: Acceptance 2, FR-003
    void memoWithoutTagsFieldStillWorksAndHasEmptyTags() throws Exception {
        create("태그 없음", null).andExpect(status().isCreated()).andExpect(jsonPath("$.tags.length()").value(0));
    }

    @Test // T010: Acceptance 3, FR-004, FR-010
    void duplicatesCaseAndWhitespaceAreNormalized() throws Exception {
        create("정규화", List.of("Meeting", " meeting ", "MEETING")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags.length()").value(1))
                .andExpect(jsonPath("$.tags[0]").value("meeting"));
    }

    @Test // T010a: FR-002 (analyze finding A1)
    void tagsAreReturnedSortedByName() throws Exception {
        long id = createAndGetId("정렬", List.of("c", "a", "b"));
        mvc.perform(get("/api/memos/" + id))
                .andExpect(jsonPath("$.tags[0]").value("a"))
                .andExpect(jsonPath("$.tags[1]").value("b"))
                .andExpect(jsonPath("$.tags[2]").value("c"));
    }

    @Test // T011: FR-008, SC-004
    void blankTagIsRejectedAndTheFieldIsNamed() throws Exception {
        create("x", List.of("ok", "")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("tags[1]"));
    }

    @Test // T011: FR-008
    void tooLongTagIsRejected() throws Exception {
        create("x", List.of("a".repeat(31))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("tags[0]"));
    }

    @Test // T011: FR-009
    void moreThanTenTagsAreRejected() throws Exception {
        List<String> eleven = IntStream.rangeClosed(1, 11).mapToObj(i -> "t" + i).toList();
        create("x", eleven).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("tags"));
    }

    // ---------------- User Story 2 ----------------

    @Test // T016: Acceptance 1, SC-003
    void filterReturnsOnlyMemosWithThatTag() throws Exception {
        create("회의 1", List.of("회의")).andExpect(status().isCreated());
        create("회의 2", List.of("회의", "아이디어")).andExpect(status().isCreated());
        create("태그 없음", null).andExpect(status().isCreated());
        mvc.perform(get("/api/memos").param("tag", "회의"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.title == '태그 없음')]").isEmpty());
    }

    @Test // T017: Acceptance 2, FR-005
    void withoutFilterAllMemosAreReturned() throws Exception {
        create("A", List.of("x")).andExpect(status().isCreated());
        create("B", null).andExpect(status().isCreated());
        mvc.perform(get("/api/memos")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test // T018: Acceptance 3, FR-006
    void unknownTagReturnsEmptyListNotAnError() throws Exception {
        create("A", List.of("x")).andExpect(status().isCreated());
        mvc.perform(get("/api/memos").param("tag", "없는태그"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test // T019: FR-010
    void filterIsCaseAndWhitespaceInsensitive() throws Exception {
        create("A", List.of("meeting")).andExpect(status().isCreated());
        create("B", List.of("other")).andExpect(status().isCreated());   // 필터가 무시되면 2개가 나와서 실패한다
        mvc.perform(get("/api/memos").param("tag", " Meeting "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("A"));
    }

    @Test // T020: FR-008 (filter value)
    void blankFilterValueIsRejected() throws Exception {
        mvc.perform(get("/api/memos").param("tag", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("tag"));
    }
}
