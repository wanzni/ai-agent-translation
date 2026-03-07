package cn.net.susan.ai.translation.controller;

import cn.net.susan.ai.translation.entity.DocumentTranslation;
import cn.net.susan.ai.translation.dto.DeleteResponse;
import cn.net.susan.ai.translation.security.UserContext;
import cn.net.susan.ai.translation.service.DocumentTranslationService;
import cn.net.susan.ai.translation.service.StorageService;
import cn.net.susan.ai.translation.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import cn.net.susan.ai.translation.dto.FileUrlResponse;
import cn.net.susan.ai.translation.dto.StartTranslationResponse;
import cn.net.susan.ai.translation.dto.DocumentProgressResponse;

/**
 * 文档翻译 API 控制器
 *
 * <p>该控制器负责处理与文档翻译相关的所有 API 请求，
 * 包括文档的上传、翻译、状态查询、下载和删除等功能。
 *
 * <p>主要功能包括：
 * <ul>
 *     <li>上传文档并启动翻译任务</li>
 *     <li>获取当前用户的文档翻译历史列表</li>
 *     <li>获取翻译后或原始文档的下载链接</li>
 *     <li>直接下载翻译后的文档</li>
 *     <li>手动启动已上传文档的翻译</li>
 *     <li>查询翻译任务的进度</li>
 *     <li>删除指定的翻译任务</li>
 * </ul>
 *
 * @author AI Assistant
 * @version 1.0.0
 * @since 2024-07-28
 */
@Slf4j
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentTranslationController {

    private final DocumentTranslationService documentTranslationService;
    private final StorageService storageService;
    private final PointsService pointsService;

    /**
     * 上传文档并立即开始翻译
     *
     * <p>此端点接收一个文件以及相关的翻译参数，然后将文件上传到存储服务，
     * 并创建一个新的文档翻译任务。任务创建后会自动开始处理。
     *
     * @param file              上传的文件
     * @param sourceLanguage    源语言
     * @param targetLanguage    目标语言
     * @param translationType   翻译类型（如 STANDARD）
     * @param translationEngine 翻译引擎（如 google）
     * @param useTerminology    是否使用术语库
     * @param priority          任务优先级（可选）
     * @return 创建的文档翻译任务实体
     * @throws Exception 上传或任务创建过程中可能发生的异常
     */
    @PostMapping("/upload")
    public DocumentTranslation uploadAndTranslate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sourceLanguage") String sourceLanguage,
            @RequestParam("targetLanguage") String targetLanguage,
            @RequestParam(value = "translationType", defaultValue = "STANDARD") String translationType,
            @RequestParam(value = "translationEngine", defaultValue = "google") String translationEngine,
            @RequestParam(value = "useTerminology", defaultValue = "false") Boolean useTerminology,
            @RequestParam(value = "priority", required = false) Integer priority
    ) throws Exception {
        // 忽略前端传入用户ID，统一由服务层从用户上下文解析
        return documentTranslationService.uploadAndTranslate(
                file, sourceLanguage, targetLanguage, translationType, translationEngine, useTerminology, priority);
    }

    /**
     * 获取当前用户的文档翻译任务列表
     *
     * <p>此端点用于分页查询当前登录用户的文档翻译历史。
     * 可以根据状态进行筛选，并按创建时间降序排序。
     *
     * @param status 任务状态（可选），用于筛选
     * @param page   分页查询的页码（从 0 开始）
     * @param size   每页的记录数
     * @return 文档翻译任务的分页结果
     * @throws Exception 查询过程中可能发生的异常
     */
    @GetMapping("/list")
    public Page<DocumentTranslation> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) throws Exception {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // 按当前登录用户过滤上传历史
        Long userId = UserContext.getUserId();
        return documentTranslationService.getDocumentTranslations(status, pageable);
    }

    /**
     * 获取翻译结果的预签名下载链接
     *
     * <p>此端点为指定的翻译任务生成一个临时的、有时效性的下载链接，
     * 用于安全地下载翻译后的文档。
     *
     * @param id 翻译任务的唯一标识符
     * @return 包含预签名 URL 和文件名的响应
     * @throws Exception 如果任务或文件不存在，或生成链接失败
     */
    @GetMapping("/download-url/{id}")
    public FileUrlResponse getDownloadUrl(@PathVariable("id") Long id) throws Exception {
        DocumentTranslation dt = documentTranslationService.getDocumentTranslationById(id);
        if (dt == null || dt.getTranslatedFilePath() == null) {
            throw new IllegalArgumentException("翻译结果不存在");
        }
        String url = storageService.getPresignedGetUrl(dt.getTranslatedFilePath(), 60 * 10);
        return FileUrlResponse.builder()
                .url(url)
                .filename(dt.getOriginalFilename())
                .build();
    }

    /**
     * 获取原始文件的预签名下载链接
     *
     * <p>此端点为指定的翻译任务生成一个临时的、有时效性的下载链接，
     * 用于安全地下载原始上传的文档。
     *
     * @param id 翻译任务的唯一标识符
     * @return 包含预签名 URL 和文件名的响应
     * @throws Exception 如果任务或文件不存在，或生成链接失败
     */
    @GetMapping("/original-url/{id}")
    public FileUrlResponse getOriginalUrl(@PathVariable("id") Long id) throws Exception {
        DocumentTranslation dt = documentTranslationService.getDocumentTranslationById(id);
        if (dt == null || dt.getSourceFilePath() == null) {
            throw new IllegalArgumentException("原始文件不存在");
        }
        String url = storageService.getPresignedGetUrl(dt.getSourceFilePath(), 60 * 10);
        return FileUrlResponse.builder()
                .url(url)
                .filename(dt.getOriginalFilename())
                .build();
    }

    /**
     * 直接下载翻译后的文档
     *
     * <p>此端点直接返回翻译后文档的字节流，并设置适当的 HTTP 头，
     * 以强制浏览器触发文件下载。适用于不希望通过预签名链接的场景。
     *
     * @param id 翻译任务的唯一标识符
     * @return 包含文件字节流和 HTTP 头的 {@link ResponseEntity}
     * @throws Exception 如果任务或文件不存在，或读取文件失败
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable("id") Long id) throws Exception {
        DocumentTranslation dt = documentTranslationService.getDocumentTranslationById(id);
        if (dt == null || dt.getTranslatedFilePath() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        byte[] bytes = documentTranslationService.downloadTranslatedDocument(id);

        String originalName = dt.getOriginalFilename();
        String safeName = originalName == null ? "translated.pdf" : "translated_" + originalName;
        String encodedFileName = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        String ext = dt.getFileType() != null ? dt.getFileType().name().toLowerCase() : "pdf";
        MediaType mediaType = "pdf".equals(ext) ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(bytes.length);
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + safeName + "\"; filename*=UTF-8''" + encodedFileName);
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * 手动开始一个已上传文档的翻译任务
     *
     * <p>此端点用于触发一个已经创建但尚未开始的翻译任务。
     * 也可以用于重新翻译一个已完成的任务，并可以覆盖翻译引擎、目标语言等参数。
     *
     * @param id                翻译任务的唯一标识符
     * @param translationEngine 新的翻译引擎（可选）
     * @param targetLanguage    新的目标语言（可选）
     * @param translationType   新的翻译类型（可选）
     * @return 包含任务信息和用户点数余额的响应
     * @throws Exception 启动任务过程中可能发生的异常
     */
    @PostMapping("/start/{id}")
    public StartTranslationResponse startTranslation(@PathVariable("id") Long id,
                                                @RequestParam(value = "translationEngine", required = false) String translationEngine,
                                                @RequestParam(value = "targetLanguage", required = false) String targetLanguage,
                                                @RequestParam(value = "translationType", required = false) String translationType) throws Exception {
        DocumentTranslation dt = documentTranslationService.startTranslation(id, translationEngine, targetLanguage, translationType);
        Long pointsBalance = null;
        try {
            Long userId = dt.getUserId();
            pointsBalance = userId == null ? null : pointsService.getBalance(userId);
        } catch (Exception e) {
            // 忽略余额查询错误，避免影响主流程
        }
        return StartTranslationResponse.builder()
                .id(dt.getId())
                .originalFilename(dt.getOriginalFilename())
                .fileSize(dt.getFileSize())
                .sourceLanguage(dt.getSourceLanguage())
                .targetLanguage(dt.getTargetLanguage())
                .translationEngine(dt.getTranslationEngine())
                .status(dt.getStatus() == null ? null : dt.getStatus().name())
                .progress(dt.getProgress())
                .createdAt(dt.getCreatedAt())
                .usedPoints(10L)
                .pointsBalance(pointsBalance)
                .build();
    }

    /**
     * 查询指定文档翻译任务的进度
     *
     * @param id 翻译任务的唯一标识符
     * @return 包含任务进度的响应
     * @throws Exception 查询过程中可能发生的异常
     */
    @GetMapping("/progress/{id}")
    public DocumentProgressResponse getProgress(@PathVariable("id") Long id) throws Exception {
        return documentTranslationService.getTranslationProgressResponse(id);
    }

    /**
     * 删除指定的文档翻译任务
     *
     * <p>此端点会删除指定的翻译任务记录及其关联的存储文件。
     *
     * @param id 要删除的翻译任务的唯一标识符
     * @return 删除操作的结果
     * @throws Exception 删除过程中可能发生的异常
     */
    @DeleteMapping("/delete/{id}")
    public DeleteResponse delete(@PathVariable("id") Long id) throws Exception {
        boolean ok = documentTranslationService.deleteDocumentTranslation(id);
        return new DeleteResponse(id, ok);
    }
}