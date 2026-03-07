package cn.net.susan.ai.translation.service.llm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RagService测试类
 */
@SpringBootTest
public class RagServiceTest {

    @Autowired
    private RagService ragService;

    /**
     * 测试中文分词效果
     */
    @Test
    public void testChineseSegmentation() throws Exception {
        // 使用反射调用私有方法
        Method extractKeywordsMethod = RagService.class.getDeclaredMethod("extractKeywords", String.class);
        extractKeywordsMethod.setAccessible(true);

        // 测试中文
        String chineseText = "机器学习在图像识别中的应用";
        List<String> chineseResult = (List<String>) extractKeywordsMethod.invoke(ragService, chineseText);

        System.out.println("中文输入: " + chineseText);
        System.out.println("分词结果: " + chineseResult);

        // 验证结果
        assertNotNull(chineseResult);
        assertTrue(chineseResult.size() > 0);
        assertTrue(chineseResult.contains("机器学习"), "应该包含'机器学习'");
        assertTrue(chineseResult.contains("图像识别"), "应该包含'图像识别'");
        assertTrue(chineseResult.contains("应用"), "应该包含'应用'");

        // 停用词应该被过滤
        assertFalse(chineseResult.contains("在"), "停用词'在'应该被过滤");
        assertFalse(chineseResult.contains("中"), "停用词'中'应该被过滤");
        assertFalse(chineseResult.contains("的"), "停用词'的'应该被过滤");
    }

    /**
     * 测试英文分词效果
     */
    @Test
    public void testEnglishSegmentation() throws Exception {
        Method extractKeywordsMethod = RagService.class.getDeclaredMethod("extractKeywords", String.class);
        extractKeywordsMethod.setAccessible(true);

        String englishText = "Machine learning is amazing for AI applications";
        List<String> englishResult = (List<String>) extractKeywordsMethod.invoke(ragService, englishText);

        System.out.println("英文输入: " + englishText);
        System.out.println("分词结果: " + englishResult);

        assertNotNull(englishResult);
        assertTrue(englishResult.size() > 0);
        assertTrue(englishResult.contains("machine"), "应该包含'machine'");
        assertTrue(englishResult.contains("learning"), "应该包含'learning'");
        assertTrue(englishResult.contains("amazing"), "应该包含'amazing'");
        assertTrue(englishResult.contains("applications"), "应该包含'applications'");

        // 停用词应该被过滤
        assertFalse(englishResult.contains("is"), "停用词'is'应该被过滤");
        assertFalse(englishResult.contains("for"), "停用词'for'应该被过滤");
    }

    /**
     * 测试中英混合文本
     */
    @Test
    public void testMixedText() throws Exception {
        Method extractKeywordsMethod = RagService.class.getDeclaredMethod("extractKeywords", String.class);
        extractKeywordsMethod.setAccessible(true);

        String mixedText = "深度学习Deep Learning在NLP中的应用";
        List<String> mixedResult = (List<String>) extractKeywordsMethod.invoke(ragService, mixedText);

        System.out.println("混合输入: " + mixedText);
        System.out.println("分词结果: " + mixedResult);

        assertNotNull(mixedResult);
        assertTrue(mixedResult.size() > 0);
        // 包含中文应该按中文分词
        assertTrue(mixedResult.contains("深度学习"), "应该包含'深度学习'");
        assertTrue(mixedResult.contains("应用"), "应该包含'应用'");
    }

    /**
     * 测试长文本
     */
    @Test
    public void testLongText() throws Exception {
        Method extractKeywordsMethod = RagService.class.getDeclaredMethod("extractKeywords", String.class);
        extractKeywordsMethod.setAccessible(true);

        String longText = "自然语言处理是人工智能领域中的一个重要方向。它研究能实现人与计算机之间用自然语言进行有效通信的各种理论和方法。自然语言处理是一门融语言学、计算机科学、数学于一体的科学。";
        List<String> longResult = (List<String>) extractKeywordsMethod.invoke(ragService, longText);

        System.out.println("长文本分词结果数量: " + longResult.size());
        System.out.println("长文本分词结果: " + longResult);

        assertNotNull(longResult);
        assertTrue(longResult.size() <= 50, "结果应该不超过50个");
        assertTrue(longResult.contains("自然语言处理"), "应该包含'自然语言处理'");
        assertTrue(longResult.contains("人工智能"), "应该包含'人工智能'");
    }

    /**
     * 测试空文本和特殊字符
     */
    @Test
    public void testEdgeCases() throws Exception {
        Method extractKeywordsMethod = RagService.class.getDeclaredMethod("extractKeywords", String.class);
        extractKeywordsMethod.setAccessible(true);

        // 空文本
        List<String> emptyResult = (List<String>) extractKeywordsMethod.invoke(ragService, "");
        assertTrue(emptyResult.isEmpty(), "空文本应该返回空列表");

        // null
        List<String> nullResult = (List<String>) extractKeywordsMethod.invoke(ragService, (String) null);
        assertTrue(nullResult.isEmpty(), "null应该返回空列表");

        // 只有停用词
        List<String> stopWordsResult = (List<String>) extractKeywordsMethod.invoke(ragService, "的 了 是");
        assertTrue(stopWordsResult.isEmpty(), "只有停用词应该返回空列表");
    }
}
