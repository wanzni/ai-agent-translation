package cn.net.wanzni.ai.translation.service.file;

/**
 * 文档翻译器工厂：根据扩展名选择策略实现。
 */
public class DocumentFileTranslatorFactory {

    public static DocumentFileTranslator forExtension(String ext) {
        if (ext == null) return new DefaultCopyTranslator();
        String lower = ext.toLowerCase();
        switch (lower) {
            case "pptx":
                return new PptxFileTranslator();
            case "ppt":
                return new PptFileTranslator();
            case "pdf":
                return new PdfFileTranslator();
            case "docx":
                return new DocxFileTranslator();
            case "doc":
                return new DocFileTranslator();
            case "xlsx":
                return new XlsxFileTranslator();
            case "xls":
                return new XlsFileTranslator();
            case "txt":
                return new TxtFileTranslator();
            // TODO: 后续扩展 pdf 等类型处理
            default:
                return new DefaultCopyTranslator();
        }
    }
}