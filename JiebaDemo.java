import com.huaban.analysis.jieba.JiebaSegmenter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * jieba中文分词演示
 */
public class JiebaDemo {
    
    private static final Set<String> stopWords = Set.of(
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
        "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
        "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "这些"
    );
    
    public static void main(String[] args) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        
        System.out.println("========== jieba中文分词演示 ==========\n");
        
        // 测试用例1
        String text1 = "机器学习在图像识别中的应用";
        System.out.println("输入: " + text1);
        System.out.println("原始分词: " + segmenter.sentenceProcess(text1));
        System.out.println("处理后: " + process(segmenter.sentenceProcess(text1)));
        System.out.println();
        
        // 测试用例2
        String text2 = "深度学习是机器学习的一个分支";
        System.out.println("输入: " + text2);
        System.out.println("原始分词: " + segmenter.sentenceProcess(text2));
        System.out.println("处理后: " + process(segmenter.sentenceProcess(text2)));
        System.out.println();
        
        // 测试用例3
        String text3 = "自然语言处理是人工智能领域中的重要方向";
        System.out.println("输入: " + text3);
        System.out.println("原始分词: " + segmenter.sentenceProcess(text3));
        System.out.println("处理后: " + process(segmenter.sentenceProcess(text3)));
        System.out.println();
        
        System.out.println("========== 演示完成 ==========");
    }
    
    private static List<String> process(List<String> words) {
        return words.stream()
            .map(String::toLowerCase)
            .filter(word -> word.length() >= 2)
            .filter(word -> !stopWords.contains(word))
            .distinct()
            .collect(Collectors.toList());
    }
}
