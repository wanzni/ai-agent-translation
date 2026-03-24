package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.config.ApiResponseAdvice;
import cn.net.wanzni.ai.translation.dto.review.ReviewQueueStatsResponse;
import cn.net.wanzni.ai.translation.dto.review.ReviewTaskResponse;
import cn.net.wanzni.ai.translation.entity.User;
import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import cn.net.wanzni.ai.translation.handler.GlobalExceptionHandler;
import cn.net.wanzni.ai.translation.security.UserContext;
import cn.net.wanzni.ai.translation.service.ReviewTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewTaskControllerTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldUsePendingAsDefaultStatusWhenListingReviewTasks() throws Exception {
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        when(reviewTaskService.list(eq(ReviewStatusEnum.PENDING), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(
                        List.of(ReviewTaskResponse.builder().id(1L).build()),
                        PageRequest.of(0, 20),
                        1
                ));

        MockMvc mockMvc = buildMockMvc(reviewTaskService);

        mockMvc.perform(get("/api/review/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1L));

        verify(reviewTaskService).list(eq(ReviewStatusEnum.PENDING), eq(null), eq(null), any());
    }

    @Test
    void shouldUseCurrentUserForMineViewWhenListingReviewTasks() throws Exception {
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        when(reviewTaskService.list(eq(ReviewStatusEnum.PENDING), eq("SENSITIVE_CONTENT"), eq(100L), any()))
                .thenReturn(new PageImpl<>(
                        List.of(ReviewTaskResponse.builder().id(2L).reviewerId(100L).build()),
                        PageRequest.of(0, 20),
                        1
                ));

        MockMvc mockMvc = buildMockMvc(reviewTaskService);
        User reviewer = new User();
        reviewer.setId(100L);
        UserContext.set(reviewer);

        mockMvc.perform(get("/api/review/tasks")
                        .param("mine", "true")
                        .param("reasonCode", "SENSITIVE_CONTENT")
                        .param("reviewerId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].reviewerId").value(100L));

        verify(reviewTaskService).list(eq(ReviewStatusEnum.PENDING), eq("SENSITIVE_CONTENT"), eq(100L), any());
    }

    @Test
    void shouldReturnPublicQueueView() throws Exception {
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        when(reviewTaskService.listPublicQueue(eq("SENSITIVE_CONTENT"), any()))
                .thenReturn(new PageImpl<>(
                        List.of(ReviewTaskResponse.builder().id(5L).build()),
                        PageRequest.of(0, 20),
                        1
                ));

        MockMvc mockMvc = buildMockMvc(reviewTaskService);

        mockMvc.perform(get("/api/review/tasks/views/public").param("reasonCode", "SENSITIVE_CONTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(5L));

        verify(reviewTaskService).listPublicQueue(eq("SENSITIVE_CONTENT"), any());
    }

    @Test
    void shouldReturnStats() throws Exception {
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        when(reviewTaskService.stats()).thenReturn(ReviewQueueStatsResponse.builder()
                .pendingCount(5L)
                .publicQueueCount(2L)
                .claimedPendingCount(3L)
                .processedCount(4L)
                .build());

        MockMvc mockMvc = buildMockMvc(reviewTaskService);

        mockMvc.perform(get("/api/review/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pendingCount").value(5L))
                .andExpect(jsonPath("$.data.publicQueueCount").value(2L));

        verify(reviewTaskService).stats();
    }

    @Test
    void shouldRejectMineViewWhenReviewerIsMissing() throws Exception {
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        MockMvc mockMvc = buildMockMvc(reviewTaskService);

        mockMvc.perform(get("/api/review/tasks").param("mine", "true"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("ILLEGAL_STATE"));

        verify(reviewTaskService, never()).list(any(), any(), any(), any());
    }

    @Test
    void shouldRejectApproveWhenReviewerIsMissing() throws Exception {
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        MockMvc mockMvc = buildMockMvc(reviewTaskService);

        mockMvc.perform(post("/api/review/tasks/1/approve").contentType(APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("ILLEGAL_STATE"));

        verify(reviewTaskService, never()).approve(any(), any());
    }

    @Test
    void shouldClaimUsingCurrentUserContext() throws Exception {
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        when(reviewTaskService.claim(3L, 100L))
                .thenReturn(ReviewTaskResponse.builder().id(3L).reviewerId(100L).build());

        MockMvc mockMvc = buildMockMvc(reviewTaskService);
        User reviewer = new User();
        reviewer.setId(100L);
        UserContext.set(reviewer);

        mockMvc.perform(post("/api/review/tasks/3/claim").contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(3L))
                .andExpect(jsonPath("$.data.reviewerId").value(100L));

        verify(reviewTaskService).claim(3L, 100L);
    }

    @Test
    void shouldReleaseUsingCurrentUserContext() throws Exception {
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        when(reviewTaskService.release(4L, 100L))
                .thenReturn(ReviewTaskResponse.builder().id(4L).build());

        MockMvc mockMvc = buildMockMvc(reviewTaskService);
        User reviewer = new User();
        reviewer.setId(100L);
        UserContext.set(reviewer);

        mockMvc.perform(post("/api/review/tasks/4/release").contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(4L));

        verify(reviewTaskService).release(4L, 100L);
    }

    @Test
    void shouldRejectClaimWhenReviewerIsMissing() throws Exception {
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        MockMvc mockMvc = buildMockMvc(reviewTaskService);

        mockMvc.perform(post("/api/review/tasks/3/claim").contentType(APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("ILLEGAL_STATE"));

        verify(reviewTaskService, never()).claim(any(), any());
    }

    @Test
    void shouldRejectReleaseWhenReviewerIsMissing() throws Exception {
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        MockMvc mockMvc = buildMockMvc(reviewTaskService);

        mockMvc.perform(post("/api/review/tasks/4/release").contentType(APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("ILLEGAL_STATE"));

        verify(reviewTaskService, never()).release(any(), any());
    }

    @Test
    void shouldReviseUsingCurrentUserContext() throws Exception {
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        when(reviewTaskService.revise(2L, "human text", 100L))
                .thenReturn(ReviewTaskResponse.builder().id(2L).build());

        MockMvc mockMvc = buildMockMvc(reviewTaskService);

        User reviewer = new User();
        reviewer.setId(100L);
        UserContext.set(reviewer);

        mockMvc.perform(post("/api/review/tasks/2/revise")
                        .contentType(APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(java.util.Map.of("finalText", "human text"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2L));

        verify(reviewTaskService).revise(2L, "human text", 100L);
    }

    private MockMvc buildMockMvc(ReviewTaskService reviewTaskService) {
        return MockMvcBuilders.standaloneSetup(new ReviewTaskController(reviewTaskService))
                .setControllerAdvice(new ApiResponseAdvice(), new GlobalExceptionHandler())
                .build();
    }
}
