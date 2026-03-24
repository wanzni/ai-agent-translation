package cn.net.wanzni.ai.translation.service;

import cn.net.wanzni.ai.translation.config.DashscopeProperties;
import cn.net.wanzni.ai.translation.dto.QualityAssessmentRequest;
import cn.net.wanzni.ai.translation.dto.QualityAssessmentResponse;
import cn.net.wanzni.ai.translation.entity.QualityAssessment;
import cn.net.wanzni.ai.translation.repository.QualityAssessmentRepository;
import cn.net.wanzni.ai.translation.service.impl.QualityAssessmentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QualityAssessmentServiceImplTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldMarkTmEligibleWhenHighScoreAndHardRulesPass() throws Exception {
        QualityAssessmentServiceImpl service = createService("""
                {"choices":[{"message":{"content":"{\\"overallScore\\":92,\\"accuracyScore\\":93,\\"fluencyScore\\":90,\\"consistencyScore\\":91,\\"completenessScore\\":92,\\"improvementSuggestions\\":[\\"ok\\"],\\"attentionPoints\\":[\\"check\\"],\\"strengths\\":[\\"good\\"],\\"assessmentDetails\\":{\\"source\\":\\"mock\\"}}"}}]}
                """);

        QualityAssessmentResponse response = service.assess(QualityAssessmentRequest.builder()
                .sourceText("The API responds in 200 ms for OpenAI GPT-4.")
                .targetText("OpenAI GPT-4 API responds in 200 ms.")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .glossaryMap(Map.of("OpenAI", "OpenAI", "GPT-4", "GPT-4", "API", "API"))
                .build());

        assertEquals(92, response.getOverallScore());
        assertEquals(100, response.getNumberScore());
        assertEquals(100, response.getTerminologyScore());
        assertEquals(100, response.getFormatScore());
        assertTrue(Boolean.TRUE.equals(response.getHardRulePassed()));
        assertTrue(Boolean.TRUE.equals(response.getTmEligible()));
        assertFalse(Boolean.TRUE.equals(response.getSensitiveContentDetected()));
        assertTrue(response.getTmRejectReasons().isEmpty());
        assertTrue(response.getAssessmentDetails().containsKey("ruleCheck"));
    }

    @Test
    void shouldRejectTmWhenOverallScoreTooLow() throws Exception {
        QualityAssessmentServiceImpl service = createService("""
                {"choices":[{"message":{"content":"{\\"overallScore\\":72,\\"accuracyScore\\":73,\\"fluencyScore\\":72,\\"consistencyScore\\":71,\\"completenessScore\\":74,\\"improvementSuggestions\\":[\\"revise\\"],\\"attentionPoints\\":[\\"check\\"],\\"strengths\\":[\\"basic\\"],\\"assessmentDetails\\":{\\"source\\":\\"mock\\"}}"}}]}
                """);

        QualityAssessmentResponse response = service.assess(QualityAssessmentRequest.builder()
                .sourceText("The service supports 3 regions.")
                .targetText("The service supports 3 regions.")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .build());

        assertFalse(Boolean.TRUE.equals(response.getTmEligible()));
        assertTrue(response.getTmRejectReasons().contains("LOW_OVERALL_SCORE"));
    }

    @Test
    void shouldRejectTmWhenNumbersMismatchOrSensitiveContentDetected() throws Exception {
        QualityAssessmentServiceImpl service = createService("""
                {"choices":[{"message":{"content":"{\\"overallScore\\":95,\\"accuracyScore\\":95,\\"fluencyScore\\":94,\\"consistencyScore\\":95,\\"completenessScore\\":95,\\"improvementSuggestions\\":[\\"none\\"],\\"attentionPoints\\":[\\"check\\"],\\"strengths\\":[\\"good\\"],\\"assessmentDetails\\":{\\"source\\":\\"mock\\"}}"}}]}
                """);

        QualityAssessmentResponse response = service.assess(QualityAssessmentRequest.builder()
                .sourceText("The violence report recorded 3 incidents.")
                .targetText("The violence report recorded 4 incidents.")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .build());

        assertFalse(Boolean.TRUE.equals(response.getTmEligible()));
        assertTrue(response.getTmRejectReasons().contains("NUMBER_OR_UNIT_MISMATCH"));
        assertTrue(response.getTmRejectReasons().contains("SENSITIVE_CONTENT"));
        assertTrue(Boolean.TRUE.equals(response.getNeedsHumanReview()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldLowerFormatScoreWhenPlaceholderOrBracketIsBroken() throws Exception {
        QualityAssessmentServiceImpl service = createService("""
                {"choices":[{"message":{"content":"{\\"overallScore\\":93,\\"accuracyScore\\":93,\\"fluencyScore\\":92,\\"consistencyScore\\":93,\\"completenessScore\\":93,\\"improvementSuggestions\\":[\\"ok\\"],\\"attentionPoints\\":[\\"check\\"],\\"strengths\\":[\\"good\\"],\\"assessmentDetails\\":{\\"source\\":\\"mock\\"}}"}}]}
                """);

        QualityAssessmentResponse response = service.assess(QualityAssessmentRequest.builder()
                .sourceText("Visit https://example.com and use ${userName} (VIP).")
                .targetText("Visit and use userName VIP.")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .build());

        assertTrue(response.getFormatScore() < 100);
        Map<String, Object> ruleCheck = (Map<String, Object>) response.getAssessmentDetails().get("ruleCheck");
        Map<String, Object> formatCheck = (Map<String, Object>) ruleCheck.get("formatCheck");
        assertNotNull(formatCheck);
        assertTrue(((java.util.List<String>) formatCheck.get("failedChecks")).contains("URL_COUNT_MISMATCH"));
        assertTrue(((java.util.List<String>) formatCheck.get("failedChecks")).contains("PLACEHOLDER_COUNT_MISMATCH"));
    }

    @Test
    void shouldPersistRealFormatScoreInsteadOfCompletenessScore() throws Exception {
        QualityAssessmentRepository repository = mock(QualityAssessmentRepository.class);
        QualityAssessmentServiceImpl service = createService("""
                {"choices":[{"message":{"content":"{\\"overallScore\\":88,\\"accuracyScore\\":90,\\"fluencyScore\\":89,\\"consistencyScore\\":87,\\"completenessScore\\":91,\\"improvementSuggestions\\":[\\"ok\\"],\\"attentionPoints\\":[\\"check\\"],\\"strengths\\":[\\"good\\"],\\"assessmentDetails\\":{\\"source\\":\\"mock\\"}}"}}]}
                """, repository);

        QualityAssessmentResponse response = service.assess(QualityAssessmentRequest.builder()
                .sourceText("User: ${name}\\nCode: `A-1`")
                .targetText("User: name\\nCode: A-1")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .translationRecordId(123L)
                .save(true)
                .build());

        ArgumentCaptor<QualityAssessment> captor = ArgumentCaptor.forClass(QualityAssessment.class);
        verify(repository).save(captor.capture());
        QualityAssessment saved = captor.getValue();
        assertEquals(response.getFormatScore(), saved.getFormatScore());
        assertNotEquals(response.getCompletenessScore(), saved.getFormatScore());
    }

    private QualityAssessmentServiceImpl createService(String responseBody) throws Exception {
        return createService(responseBody, mock(QualityAssessmentRepository.class));
    }

    private QualityAssessmentServiceImpl createService(String responseBody,
                                                       QualityAssessmentRepository repository) throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/quality", exchange -> {
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });
        server.start();

        DashscopeProperties properties = new DashscopeProperties();
        properties.setApiKey("dummy-key");
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/quality");

        return new QualityAssessmentServiceImpl(
                properties,
                repository,
                new ObjectMapper()
        );
    }
}
