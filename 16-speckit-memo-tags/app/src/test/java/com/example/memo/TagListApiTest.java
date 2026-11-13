package com.example.memo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TagListApiTest {

    @Autowired MockMvc mvc;
    @Autowired MemoRepository repository;
    @Autowired ObjectMapper json;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private long create(String title, List<String> tags) throws Exception {
        String res = mvc.perform(post("/api/memos").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", title, "tags", tags))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    @Test // T025: Acceptance 1, FR-007
    void listsTagsWithCountsSortedByName() throws Exception {
        create("m1", List.of("회의", "weekly"));
        create("m2", List.of("회의"));
        create("m3", List.of("아이디어"));
        mvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("weekly"))
                .andExpect(jsonPath("$[0].count").value(1))
                .andExpect(jsonPath("$[1].name").value("아이디어"))
                .andExpect(jsonPath("$[1].count").value(1))
                .andExpect(jsonPath("$[2].name").value("회의"))
                .andExpect(jsonPath("$[2].count").value(2));
    }

    @Test // T026: Acceptance 2
    void noMemosMeansEmptyList() throws Exception {
        mvc.perform(get("/api/tags")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test // T027: Edge Cases (삭제하면 그 메모에만 있던 태그가 사라진다)
    void deletingTheOnlyMemoRemovesItsTagFromTheList() throws Exception {
        long id = create("only", List.of("solo"));
        create("other", List.of("kept"));
        mvc.perform(delete("/api/memos/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tags"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("kept"));
    }
}
