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

/** Constitution II(기존 필드 유지) 와 IV(문제 필드 명시) 를 함께 고정한다. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiErrorBodyTest {

    @Autowired MockMvc mvc;

    @Test
    void validationErrorNamesTheFieldAndKeepsTheLegacyFields() throws Exception {
        mvc.perform(post("/api/memos").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                // 기존 오류 본문의 필드
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/memos"))
                // 새로 추가되는 필드
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }
}
