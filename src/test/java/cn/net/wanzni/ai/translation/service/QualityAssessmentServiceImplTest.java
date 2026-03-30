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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QualityAssessmentServiceImplTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldUseJsonSchemaModeWhenDirectJsonIsValid() throws Exception {
        QualityAssessmentServiceImpl service = createService(List.of(
                script(200, completion(json("""
                        {
                          "overallScore": 92,
                          "accuracyScore": 93,
                          "fluencyScore": 90,
                          "consistencyScore": 91,
                          "completenessScore": 92,
                          "improvementSuggestions": ["ok"],
                          "attentionPoints": ["check"],
                          "strengths": ["good"],
                          "assessmentDetails": {"source": "mock"}
                        }
                        """)), body -> assertTrue(body.contains("\"type\":\"json_schema\"")))
        ));

        QualityAssessmentResponse response = service.assess(baseRequestBuilder()
                .sourceText("The API responds in 200 ms for OpenAI GPT-4.")
                .targetText("OpenAI GPT-4 API responds in 200 ms.")
                .glossaryMap(Map.of("OpenAI", "OpenAI", "GPT-4", "GPT-4", "API", "API"))
                .build());

        assertEquals(92, response.getOverallScore());
        assertEquals("JSON_SCHEMA", response.getAssessmentDetails().get("structuredOutputMode"));
        assertEquals("DIRECT", response.getAssessmentDetails().get("structuredOutputPath"));
        assertEquals(Boolean.TRUE, response.getAssessmentDetails().get("structuredOutputValid"));
        assertEquals(100, response.getNumberScore());
        assertEquals(100, response.getTerminologyScore());
    }

    @Test
    void shouldExtractJsonWrappedByExplanationOrCodeFence() throws Exception {
        String wrapped = """
                Here is the JSON result:
                ```json
                {
                  "overallScore": 88,
                  "accuracyScore": 87,
                  "fluencyScore": 88,
                  "consistencyScore": 89,
                  "completenessScore": 86,
                  "improvementSuggestions": ["revise"],
                  "attentionPoints": ["check"],
                  "strengths": ["good"],
                  "assessmentDetails": {"source": "wrapped"}
                }
                ```
                """;
        QualityAssessmentServiceImpl service = createService(List.of(
                script(200, completion(wrapped), null)
        ));

        QualityAssessmentResponse response = service.assess(baseRequestBuilder().build());

        assertEquals(88, response.getOverallScore());
        assertEquals("EXTRACTED", response.getAssessmentDetails().get("structuredOutputPath"));
        assertEquals(Boolean.TRUE, response.getAssessmentDetails().get("structuredOutputValid"));
    }

    @Test
    void shouldRepairSmallJsonErrorsBeforeFallback() throws Exception {
        String broken = """
                {
                  overallScore: 89,
                  "accuracyScore": 90,
                  "fluencyScore": 88,
                  "consistencyScore": 87,
                  "completenessScore": 89,
                  "improvementSuggestions": ["ok",],
                  "attentionPoints": ["check"],
                  "strengths": ["good"],
                  "assessmentDetails": {"source": "repair",}
                }
                """;
        QualityAssessmentServiceImpl service = createService(List.of(
                script(200, completion(broken), null)
        ));

        QualityAssessmentResponse response = service.assess(baseRequestBuilder().build());

        assertEquals(89, response.getOverallScore());
        assertEquals("REPAIRED", response.getAssessmentDetails().get("structuredOutputPath"));
        assertEquals(Boolean.TRUE, response.getAssessmentDetails().get("structuredOutputRepairApplied"));
    }

    @Test
    void shouldRetryOnceWhenStructuredPayloadTypeIsInvalid() throws Exception {
        QualityAssessmentServiceImpl service = createService(List.of(
                script(200, completion(json("""
                        {
                          "overallScore": 90,
                          "accuracyScore": "90",
                          "fluencyScore": 89,
                          "consistencyScore": 88,
                          "completenessScore": 87,
                          "improvementSuggestions": ["first"],
                          "attentionPoints": ["check"],
                          "strengths": ["good"],
                          "assessmentDetails": {"source": "invalid-type"}
                        }
                        """)), body -> assertTrue(body.contains("\"type\":\"json_schema\""))),
                script(200, completion(json("""
                        {
                          "overallScore": 91,
                          "accuracyScore": 91,
                          "fluencyScore": 90,
                          "consistencyScore": 89,
                          "completenessScore": 88,
                          "improvementSuggestions": ["fixed"],
                          "attentionPoints": ["check"],
                          "strengths": ["good"],
                          "assessmentDetails": {"source": "retry"}
                        }
                        """)), body -> {
                    assertTrue(body.contains("Fix the following model output"));
                    assertTrue(body.contains("\"type\":\"json_object\"") || !body.contains("\"response_format\""));
                })
        ));

        QualityAssessmentResponse response = service.assess(baseRequestBuilder().build());

        assertEquals(91, response.getOverallScore());
        assertEquals("RETRIED", response.getAssessmentDetails().get("structuredOutputPath"));
        assertEquals(Boolean.TRUE, response.getAssessmentDetails().get("structuredOutputRetryUsed"));
    }

    @Test
    void shouldFallbackToHeuristicWhenContentIsUnrecoverable() throws Exception {
        QualityAssessmentServiceImpl service = createService(List.of(
                script(200, completion("not-json-at-all"), null),
                script(200, completion("still-not-json"), body -> assertTrue(body.contains("Fix the following model output")))
        ));

        QualityAssessmentResponse response = service.assess(baseRequestBuilder().build());

        assertEquals("HEURISTIC", response.getAssessmentDetails().get("structuredOutputMode"));
        assertEquals("FALLBACK", response.getAssessmentDetails().get("structuredOutputPath"));
        assertEquals(Boolean.FALSE, response.getAssessmentDetails().get("structuredOutputValid"));
        assertEquals("PARSE_FAILED", response.getAssessmentDetails().get("fallbackReason"));
    }

    @Test
    void shouldFallbackFromJsonSchemaToJsonObjectWhenSchemaModeUnsupported() throws Exception {
        QualityAssessmentServiceImpl service = createService(List.of(
                script(400, "{\"message\":\"response_format json_schema unsupported\"}", body -> assertTrue(body.contains("\"type\":\"json_schema\""))),
                script(200, completion(json("""
                        {
                          "overallScore": 84,
                          "accuracyScore": 84,
                          "fluencyScore": 83,
                          "consistencyScore": 82,
                          "completenessScore": 81,
                          "improvementSuggestions": ["ok"],
                          "attentionPoints": ["check"],
                          "strengths": ["steady"],
                          "assessmentDetails": {"source": "json-object"}
                        }
                        """)), body -> assertTrue(body.contains("\"type\":\"json_object\"")))
        ));

        QualityAssessmentResponse response = service.assess(baseRequestBuilder().build());

        assertEquals(84, response.getOverallScore());
        assertEquals("JSON_OBJECT", response.getAssessmentDetails().get("structuredOutputMode"));
        assertEquals("DIRECT", response.getAssessmentDetails().get("structuredOutputPath"));
    }

    @Test
    void shouldFallbackToPromptOnlyWhenJsonObjectAlsoUnsupported() throws Exception {
        QualityAssessmentServiceImpl service = createService(List.of(
                script(400, "{\"message\":\"response_format json_schema unsupported\"}", body -> assertTrue(body.contains("\"type\":\"json_schema\""))),
                script(400, "{\"message\":\"response_format json_object unsupported\"}", body -> assertTrue(body.contains("\"type\":\"json_object\""))),
                script(200, completion(json("""
                        {
                          "overallScore": 86,
                          "accuracyScore": 85,
                          "fluencyScore": 86,
                          "consistencyScore": 87,
                          "completenessScore": 88,
                          "improvementSuggestions": ["ok"],
                          "attentionPoints": ["check"],
                          "strengths": ["prompt"],
                          "assessmentDetails": {"source": "prompt-only"}
                        }
                        """)), body -> assertFalse(body.contains("\"response_format\"")))
        ));

        QualityAssessmentResponse response = service.assess(baseRequestBuilder().build());

        assertEquals(86, response.getOverallScore());
        assertEquals("PROMPT_ONLY", response.getAssessmentDetails().get("structuredOutputMode"));
    }

    @Test
    void shouldRejectTmWhenOverallScoreTooLow() throws Exception {
        QualityAssessmentServiceImpl service = createService(List.of(
                script(200, completion(json("""
                        {
                          "overallScore": 72,
                          "accuracyScore": 73,
                          "fluencyScore": 72,
                          "consistencyScore": 71,
                          "completenessScore": 74,
                          "improvementSuggestions": ["revise"],
                          "attentionPoints": ["check"],
                          "strengths": ["basic"],
                          "assessmentDetails": {"source": "mock"}
                        }
                        """)), null)
        ));

        QualityAssessmentResponse response = service.assess(baseRequestBuilder()
                .sourceText("The service supports 3 regions.")
                .targetText("The service supports 3 regions.")
                .build());

        assertFalse(Boolean.TRUE.equals(response.getTmEligible()));
        assertTrue(response.getTmRejectReasons().contains("LOW_OVERALL_SCORE"));
    }

    @Test
    void shouldRejectTmWhenNumbersMismatchOrSensitiveContentDetected() throws Exception {
        QualityAssessmentServiceImpl service = createService(List.of(
                script(200, completion(json("""
                        {
                          "overallScore": 95,
                          "accuracyScore": 95,
                          "fluencyScore": 94,
                          "consistencyScore": 95,
                          "completenessScore": 95,
                          "improvementSuggestions": ["none"],
                          "attentionPoints": ["check"],
                          "strengths": ["good"],
                          "assessmentDetails": {"source": "mock"}
                        }
                        """)), null)
        ));

        QualityAssessmentResponse response = service.assess(baseRequestBuilder()
                .sourceText("The violence report recorded 3 incidents.")
                .targetText("The violence report recorded 4 incidents.")
                .build());

        assertFalse(Boolean.TRUE.equals(response.getTmEligible()));
        assertTrue(response.getTmRejectReasons().contains("NUMBER_OR_UNIT_MISMATCH"));
        assertTrue(response.getTmRejectReasons().contains("SENSITIVE_CONTENT"));
        assertTrue(Boolean.TRUE.equals(response.getNeedsHumanReview()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldLowerFormatScoreWhenPlaceholderOrBracketIsBroken() throws Exception {
        QualityAssessmentServiceImpl service = createService(List.of(
                script(200, completion(json("""
                        {
                          "overallScore": 93,
                          "accuracyScore": 93,
                          "fluencyScore": 92,
                          "consistencyScore": 93,
                          "completenessScore": 93,
                          "improvementSuggestions": ["ok"],
                          "attentionPoints": ["check"],
                          "strengths": ["good"],
                          "assessmentDetails": {"source": "mock"}
                        }
                        """)), null)
        ));

        QualityAssessmentResponse response = service.assess(baseRequestBuilder()
                .sourceText("Visit https://example.com and use ${userName} (VIP).")
                .targetText("Visit and use userName VIP.")
                .build());

        assertTrue(response.getFormatScore() < 100);
        Map<String, Object> ruleCheck = (Map<String, Object>) response.getAssessmentDetails().get("ruleCheck");
        Map<String, Object> formatCheck = (Map<String, Object>) ruleCheck.get("formatCheck");
        assertNotNull(formatCheck);
        assertTrue(((List<String>) formatCheck.get("failedChecks")).contains("URL_COUNT_MISMATCH"));
        assertTrue(((List<String>) formatCheck.get("failedChecks")).contains("PLACEHOLDER_COUNT_MISMATCH"));
    }

    @Test
    void shouldPersistRealFormatScoreInsteadOfCompletenessScore() throws Exception {
        QualityAssessmentRepository repository = mock(QualityAssessmentRepository.class);
        QualityAssessmentServiceImpl service = createService(List.of(
                script(200, completion(json("""
                        {
                          "overallScore": 88,
                          "accuracyScore": 90,
                          "fluencyScore": 89,
                          "consistencyScore": 87,
                          "completenessScore": 91,
                          "improvementSuggestions": ["ok"],
                          "attentionPoints": ["check"],
                          "strengths": ["good"],
                          "assessmentDetails": {"source": "mock"}
                        }
                        """)), null)
        ), repository);

        QualityAssessmentResponse response = service.assess(baseRequestBuilder()
                .sourceText("User: ${name}\nCode: `A-1`")
                .targetText("User: name\nCode: A-1")
                .translationRecordId(123L)
                .save(true)
                .build());

        ArgumentCaptor<QualityAssessment> captor = ArgumentCaptor.forClass(QualityAssessment.class);
        verify(repository).save(captor.capture());
        QualityAssessment saved = captor.getValue();
        assertEquals(response.getFormatScore(), saved.getFormatScore());
        assertNotEquals(response.getCompletenessScore(), saved.getFormatScore());
    }

    private QualityAssessmentRequest.QualityAssessmentRequestBuilder baseRequestBuilder() {
        return QualityAssessmentRequest.builder()
                .sourceText("The API responds in 200 ms.")
                .targetText("The API responds in 200 ms.")
                .sourceLanguage("en")
                .targetLanguage("zh");
    }

    private QualityAssessmentServiceImpl createService(List<ResponseScript> scripts) throws Exception {
        return createService(scripts, mock(QualityAssessmentRepository.class));
    }

    private QualityAssessmentServiceImpl createService(List<ResponseScript> scripts,
                                                       QualityAssessmentRepository repository) throws Exception {
        AtomicInteger cursor = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/quality", exchange -> {
            int index = Math.min(cursor.getAndIncrement(), scripts.size() - 1);
            ResponseScript script = scripts.get(index);
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (script.requestAssert != null) {
                script.requestAssert.accept(requestBody);
            }
            byte[] responseBytes = script.body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", script.status >= 400 ? "application/json" : "application/json");
            exchange.sendResponseHeaders(script.status, responseBytes.length);
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

    private static ResponseScript script(int status, String body, Consumer<String> requestAssert) {
        return new ResponseScript(status, body, requestAssert);
    }

    private static String completion(String content) throws Exception {
        return """
                {"choices":[{"message":{"content":%s}}]}
                """.formatted(OBJECT_MAPPER.writeValueAsString(content));
    }

    private static String json(String raw) {
        return raw.strip();
    }

    private record ResponseScript(int status, String body, Consumer<String> requestAssert) {
    }
}
