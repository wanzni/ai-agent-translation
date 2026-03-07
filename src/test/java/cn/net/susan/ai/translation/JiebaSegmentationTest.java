package cn.net.susan.ai.translation;

import com.huaban.analysis.jieba.JiebaSegmenter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * jieba中文分词测试
 */
public class JiebaSegmentationTest {

    private final JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();

    private final Set<String> stopWords = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
            "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "这些",
            "the", "is", "at", "which", "on", "a", "an", "as", "are", "was"
    );

    @BeforeAll
    public static void setup() {
        // 设置UTF-8编码，解决Windows控制台乱码
        System.setOut(new PrintStream(System.out, true, Charset.forName("UTF-8")));
        System.setErr(new PrintStream(System.err, true, Charset.forName("UTF-8")));
    }

    @Test
    public void testChineseSegmentation() {
        System.out.println("========== 中文分词测试 ==========\n");

        // 测试用例1：简单句子
        String text1 = "机器学习在图像识别中的应用";
        System.out.println("输入: " + text1);
        List<String> result1 = segment(text1);
        System.out.println("分词结果: " + result1);
        System.out.println("包含'机器学习': " + result1.contains("机器学习"));
        System.out.println("包含'图像识别': " + result1.contains("图像识别"));
        System.out.println();

        // 测试用例2：技术文章
        String text2 = "深度学习是机器学习的一个分支，它使用多层神经网络来模拟人脑的学习过程";
        System.out.println("输入: " + text2);
        List<String> result2 = segment(text2);
        System.out.println("分词结果: " + result2);
        System.out.println("包含'深度学习': " + result2.contains("深度学习"));
        System.out.println("包含'神经网络': " + result2.contains("神经网络"));
        System.out.println();

        // 测试用例3：长文本
        String text3 = "自然语言处理是人工智能领域中的一个重要方向。它研究能实现人与计算机之间用自然语言进行有效通信的各种理论和方法。";
        System.out.println("输入: " + text3.substring(0, 30) + "...");
        List<String> result3 = segment(text3);
        System.out.println("分词结果数量: " + result3.size());
        System.out.println("分词结果: " + result3);
        System.out.println();

        // 测试用例4：中英混合
        String text4 = "深度学习Deep Learning在NLP中的应用";
        System.out.println("输入: " + text4);
        List<String> result4 = segment(text4);
        System.out.println("分词结果: " + result4);
        System.out.println();

        System.out.println("========== 测试完成 ==========");
    }

    private List<String> segment(String text) {
        List<String> words = jiebaSegmenter.sentenceProcess(text);
        return words.stream()
                .map(String::toLowerCase)
                .filter(word -> word.length() >= 2)
                .filter(word -> !stopWords.contains(word))
                .distinct()
                .limit(50)
                .collect(Collectors.toList());
    }
}
