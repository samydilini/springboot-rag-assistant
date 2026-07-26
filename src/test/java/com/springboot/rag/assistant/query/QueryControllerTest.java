package com.springboot.rag.assistant.query;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.springboot.rag.assistant.query.dto.QueryResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(QueryController.class)
class QueryControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    QueryService queryService;

    @Test
    void blankQuestionReturns400WithFieldError() throws Exception {
        mvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.question").value("question must not be blank"));
    }

    @Test
    void validQuestionReturns200() throws Exception {
        when(queryService.answer("hello")).thenReturn(new QueryResponse("hi there", List.of()));

        mvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("hi there"));
    }

    @Test
    void generationUnavailableReturns503Json() throws Exception {
        when(queryService.answer(eq("q")))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Answer generation failed"));

        mvc.perform(post("/api/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"q\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Answer generation failed"));
    }
}
