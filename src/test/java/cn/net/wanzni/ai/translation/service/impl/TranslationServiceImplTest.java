package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.config.PointsProperties;
import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import cn.net.wanzni.ai.translation.repository.TranslationRecordRepository;
import cn.net.wanzni.ai.translation.service.MembershipService;
import cn.net.wanzni.ai.translation.service.PointsService;
import cn.net.wanzni.ai.translation.service.llm.RagService;
import cn.net.wanzni.ai.translation.service.translation.AliyunTranslationService;
import cn.net.wanzni.ai.translation.service.translation.QwenTranslationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;

class TranslationServiceImplTest {

    @Test
    void parallelBatchTranslate_shouldRetryOnlyTimedOutRequestsInChunk() throws Exception {
        TestableTranslationServiceImpl service = new TestableTranslationServiceImpl();

        List<TranslationRequest> requests = List.of(
                request("fast-1"),
                request("slow"),
                request("fast-2")
        );

        List<TranslationResponse> responses = service.parallelBatchTranslate(requests);

        Assertions.assertEquals(3, responses.size());
        Assertions.assertEquals("translated-fast-1-call-1", responses.get(0).getTranslatedText());
        Assertions.assertEquals("translated-slow-call-2", responses.get(1).getTranslatedText());
        Assertions.assertEquals("translated-fast-2-call-1", responses.get(2).getTranslatedText());
        Assertions.assertEquals(1, service.invocationCount("fast-1"));
        Assertions.assertEquals(2, service.invocationCount("slow"));
        Assertions.assertEquals(1, service.invocationCount("fast-2"));
    }

    private static TranslationRequest request(String sourceText) {
        return TranslationRequest.builder()
                .sourceText(sourceText)
                .sourceLanguage("en")
                .targetLanguage("zh")
                .translationEngine("MOCK")
                .translationType("TEXT")
                .build();
    }

    private static final class TestableTranslationServiceImpl extends TranslationServiceImpl {

        private final Map<String, AtomicInteger> invocations = new ConcurrentHashMap<>();
        private final Executor executor = Executors.newFixedThreadPool(2);

        private TestableTranslationServiceImpl() {
            super(
                    mock(TranslationRecordRepository.class),
                    mock(AliyunTranslationService.class),
                    mock(QwenTranslationService.class),
                    mock(RagService.class),
                    mock(PointsService.class),
                    mock(MembershipService.class),
                    mock(PointsProperties.class),
                    new ObjectMapper()
            );
        }

        @Override
        protected int getParallelBatchChunkSize() {
            return 2;
        }

        @Override
        protected long getParallelBatchTimeoutMillis(int chunkRequestCount) {
            return 60L;
        }

        @Override
        protected Executor getParallelTranslateExecutor() {
            return executor;
        }

        @Override
        public TranslationResponse translate(TranslationRequest request) throws Exception {
            int call = invocations
                    .computeIfAbsent(request.getSourceText(), key -> new AtomicInteger())
                    .incrementAndGet();

            if ("slow".equals(request.getSourceText()) && call == 1) {
                Thread.sleep(120L);
            }

            return TranslationResponse.builder()
                    .sourceText(request.getSourceText())
                    .sourceLanguage(request.getSourceLanguage())
                    .targetLanguage(request.getTargetLanguage())
                    .translatedText("translated-" + request.getSourceText() + "-call-" + call)
                    .status("COMPLETED")
                    .build();
        }

        private int invocationCount(String sourceText) {
            AtomicInteger counter = invocations.get(sourceText);
            return counter == null ? 0 : counter.get();
        }
    }
}
