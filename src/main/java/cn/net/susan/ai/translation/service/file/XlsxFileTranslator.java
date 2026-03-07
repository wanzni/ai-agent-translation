package cn.net.susan.ai.translation.service.file;

import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.dto.TranslationResponse;
import cn.net.susan.ai.translation.entity.DocumentTranslation;
import cn.net.susan.ai.translation.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * XLSX 翻译器：遍历工作簿的文本单元格进行批量翻译，保留单元格样式。
 */
@Slf4j
public class XlsxFileTranslator implements DocumentFileTranslator {

    @Override
    public byte[] translate(byte[] sourceBytes,
                             DocumentTranslation task,
                             TranslationService translationService,
                             BiConsumer<Integer, String> progressCallback) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(sourceBytes);
             XSSFWorkbook workbook = new XSSFWorkbook(bais)) {

            List<Cell> targetCells = new ArrayList<>();
            List<TranslationRequest> requests = new ArrayList<>();

            int sheetCount = workbook.getNumberOfSheets();
            int scanned = 0;
            for (int si = 0; si < sheetCount; si++) {
                Sheet sheet = workbook.getSheetAt(si);
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        scanned++;
                        String text = extractString(cell);
                        if (text != null && !text.trim().isEmpty()) {
                            targetCells.add(cell);
                            requests.add(TranslationRequest.builder()
                                    .sourceText(text)
                                    .sourceLanguage(task.getSourceLanguage() == null ? "auto" : task.getSourceLanguage())
                                    .targetLanguage(task.getTargetLanguage())
                                    .translationType("TEXT")
                                    .translationEngine(task.getTranslationEngine())
                                    .build());
                        }
                        if (scanned % 200 == 0) {
                            int percent = Math.min(50, 20 + (scanned / 200));
                            progressCallback.accept(percent, "扫描单元格中(" + scanned + ")");
                        }
                    }
                }
            }

            if (requests.isEmpty()) {
                progressCallback.accept(60, "Excel未发现可翻译文本，原样输出");
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    workbook.write(baos);
                    return baos.toByteArray();
                }
            }

            progressCallback.accept(55, "批量翻译中，共" + requests.size() + "个单元格");
            List<TranslationResponse> responses = translationService.batchTranslate(requests);

            int applied = 0;
            for (int i = 0; i < targetCells.size(); i++) {
                Cell cell = targetCells.get(i);
                String translated = null;
                if (responses != null && i < responses.size() && responses.get(i) != null) {
                    translated = responses.get(i).getTranslatedText();
                }
                if (translated == null || translated.isBlank()) {
                    translated = "[" + task.getTargetLanguage() + "] " + requests.get(i).getSourceText();
                }
                setCellString(cell, translated);
                applied++;
                if (applied % 100 == 0) {
                    int percent = 60 + (int) Math.round(applied * 25.0 / requests.size());
                    progressCallback.accept(Math.min(85, percent), "已替换译文(" + applied + "/" + requests.size() + ")");
                }
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                workbook.write(baos);
                progressCallback.accept(90, "Excel内容处理完成，准备上传");
                return baos.toByteArray();
            }
        }
    }

    private String extractString(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType();
        switch (type) {
            case STRING:
                return cell.getStringCellValue();
            case FORMULA:
                if (cell.getCachedFormulaResultType() == CellType.STRING) {
                    return cell.getStringCellValue();
                } else {
                    return null;
                }
            default:
                return null;
        }
    }

    private void setCellString(Cell cell, String value) {
        // 仅替换内容，不修改单元格样式，确保字体/边框/填充等保持不变
        cell.setCellValue(value);
    }
}