package com.example.memo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MalformedBodyTest {

    @Autowired MockMvc mvc;

    @Test // T034: G1, SC-004
    void tagsOfTheWrongTypeAreRejectedAndTheFieldIsNamed() throws Exception {
        mvc.perform(post("/api/memos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"tags\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("tags"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test // T035: G2
    void brokenJsonIsRejectedWithABodyError() throws Exception {
        mvc.perform(post("/api/memos").contentType(MediaType.APPLICATION_JSON).content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("body"));
    }
}
