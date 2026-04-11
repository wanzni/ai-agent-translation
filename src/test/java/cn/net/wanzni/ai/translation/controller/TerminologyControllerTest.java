package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.config.ApiResponseAdvice;
import cn.net.wanzni.ai.translation.dto.TerminologyStatsResponse;
import cn.net.wanzni.ai.translation.entity.User;
import cn.net.wanzni.ai.translation.handler.GlobalExceptionHandler;
import cn.net.wanzni.ai.translation.security.UserContext;
import cn.net.wanzni.ai.translation.service.TerminologyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TerminologyControllerTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldUseCurrentUserContextForStatsWhenAuthorizationHeaderContainsJwt() throws Exception {
        TerminologyService terminologyService = mock(TerminologyService.class);
        when(terminologyService.getTerminologyStatisticsResponse("8"))
                .thenReturn(TerminologyStatsResponse.builder().totalEntries(20L).totalTerms(20L).categoryCount(2).build());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TerminologyController(terminologyService))
                .setControllerAdvice(new ApiResponseAdvice(), new GlobalExceptionHandler())
                .build();

        User user = new User();
        user.setId(8L);
        UserContext.set(user);

        mockMvc.perform(get("/api/terminology/stats")
                        .header("Authorization", "Bearer eyJ.fake.jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEntries").value(20L));

        verify(terminologyService).getTerminologyStatisticsResponse("8");
    }
}