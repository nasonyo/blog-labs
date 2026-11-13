package com.example.memo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 태그 기능과 무관한 기존 동작의 기준선 (SC-002). 이후 모든 단계에서 계속 통과해야 한다. */
@SpringBootTest
@AutoConfigureMockMvc
class MemoBaselineTest {

    @Autowired MockMvc mvc;
    @Autowired MemoRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void createWithoutTagsStillReturns201() throws Exception {
        mvc.perform(post("/api/memos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"기존 방식\",\"content\":\"본문\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("기존 방식"))
                .andExpect(jsonPath("$.content").value("본문"));
    }

    @Test
    void listReturnsCreatedMemos() throws Exception {
        mvc.perform(post("/api/memos").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"하나\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/memos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unknownMemoReturns404() throws Exception {
        mvc.perform(get("/api/memos/999999")).andExpect(status().isNotFound());
    }
}
