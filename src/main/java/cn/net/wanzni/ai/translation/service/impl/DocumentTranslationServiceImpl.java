package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.dto.*;
import cn.net.wanzni.ai.translation.dto.*;
import cn.net.wanzni.ai.translation.entity.DocumentTranslation;
import cn.net.wanzni.ai.translation.enums.*;
import cn.net.wanzni.ai.translation.enums.*;
import cn.net.wanzni.ai.translation.repository.DocumentTranslationRepository;
import cn.net.wanzni.ai.translation.repository.FileStorageRepository;
import cn.net.wanzni.ai.translation.entity.FileStorage;
import cn.net.wanzni.ai.translation.repository.PointsTransactionRepository;
import cn.net.wanzni.ai.translation.repository.QualityAssessmentRepository;
import cn.net.wanzni.ai.translation.service.StorageService;
import cn.net.wanzni.ai.translation.service.TranslationService;
import cn.net.wanzni.ai.translation.service.PointsService;
import cn.net.wanzni.ai.translation.service.MembershipService;
import cn.net.wanzni.ai.translation.config.PointsProperties;
import cn.net.wanzni.ai.translation.service.DocumentTranslationService;
import cn.net.wanzni.ai.translation.security.UserContext;
import cn.net.wanzni.ai.translation.exception.InsufficientPointsException;

import cn.net.wanzni.ai.translation.util.UserContextUtils;
import cn.net.wanzni.ai.translation.service.file.DocumentFileTranslator;
import cn.net.wanzni.ai.translation.service.file.DocumentFileTranslatorFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档翻译服务实现类，提供文档翻译功能的具体实现。
 */
@Service
@RequiredArgsConstructor
public class DocumentTranslationServiceImpl implements DocumentTranslationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentTranslationServiceImpl.class);

    private final DocumentTranslationRepository documentTranslationRepository;
    private final StorageService storageService;
    private final FileStorageRepository fileStorageRepository;
    private final TranslationService translationService;
    private final PointsService pointsService;
    private final MembershipService membershipService;
    private final PointsTransactionRepository pointsTransactionRepository;
    private final PointsProperties pointsProperties;
    private final QualityAssessmentRepository qualityAssessmentRepository;

    // 支持的文档类型
    private static final Map<String, String> SUPPORTED_DOCUMENT_TYPES = Map.of(
            "pdf", "PDF文档",
            "docx", "Word文档",
            "doc", "Word文档(旧版)",
            "txt", "纯文本文件",
            "rtf", "富文本格式",
            "xlsx", "Excel表格",
            "xls", "Excel表格(旧版)",
            "pptx", "PowerPoint演示文稿",
            "ppt", "PowerPoint演示文稿(旧版)",
            "html", "HTML网页"
    );

    /**
     * 运行版本号，用于在用户多次点击“开始翻译”时安全重启任务。
     * 每次调用 startTranslation 都会递增对应任务的版本号，旧线程检测到版本号变化后主动退出。
     */
    private final Map<Long, Integer> taskRunVersion = new ConcurrentHashMap<>();

    /**
     * 上传并开始翻译文档。
     *
     * @param file 上传的文档文件
     * @param sourceLanguage 源语言代码
     * @param targetLanguage 目标语言代码
     * @param translationType 翻译类型
     * @param translationEngine 翻译引擎
     * @param useTerminology 是否使用术语库
     * @param priority 优先级
     * @return 文档翻译任务信息
     * @throws Exception 翻译过程中的异常
     */
    @Override
    public DocumentTranslation uploadAndTranslate(
            MultipartFile file,
            String sourceLanguage,
            String targetLanguage,
            String translationType,
            String translationEngine,
            Boolean useTerminology,
            Integer priority
    ) throws Exception {

        log.info("开始上传并翻译文档: filename={}, sourceLanguage={}, targetLanguage={}",
                file.getOriginalFilename(), sourceLanguage, targetLanguage);

        // 验证文件格式
        if (!isDocumentFormatSupported(file.getOriginalFilename(), file.getContentType())) {
            throw new IllegalArgumentException("不支持的文档格式");
        }
        long startTime = System.currentTimeMillis();

        // 生成对象名
        String ext = getFileExtension(file.getOriginalFilename());
        String uuid = UUID.randomUUID().toString();
        String baseName = uuid + "-" + file.getOriginalFilename();
        String originalObject = "original/" + baseName;
        // 译文对象名在开始翻译时再生成，这里仅处理原文件上传

        // 上传原文件到 MinIO
        storageService.upload(file, originalObject);

        // 从用户上下文填充用户ID（后端统一拦截器注入）
        Long effectiveUserId = UserContext.getUserId();

        // 创建文档翻译任务（上传完成后标记为 待处理PENDING）
        DocumentTranslation documentTranslation = DocumentTranslation.builder()
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileType(DocumentTypeEnum.valueOf(ext.toUpperCase()))
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .translationType(TranslationTypeEnum.valueOf(translationType))
                .translationEngine(translationEngine)
                .useTerminology(useTerminology != null ? useTerminology : false)
                .userId(effectiveUserId)
                .priority(priority != null ? priority : 5)
                .status(ProcessingStatusEnum.PENDING)
                .progress(0)
                .statusMessage("文档上传成功，待翻译")
                .sourceFilePath(originalObject)
                .startTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        DocumentTranslation savedDocument = documentTranslationRepository.save(documentTranslation);

        // 保存文件存储元数据（原文件与译文）
        FileStorage originalMeta = FileStorage.builder()
                .fileName(baseName)
                .originalName(file.getOriginalFilename())
                .filePath(originalObject)
                .fileSize(file.getSize())
                .fileType(ext)
                .mimeType(file.getContentType())
                .storageType(StorageTypeEnum.MINIO)
                .userId(effectiveUserId)
                .isPublic(false)
                .build();
        fileStorageRepository.save(originalMeta);

        // 取消在上传阶段扣点，改为在开始翻译阶段进行扣点

        log.info("文档上传完成，任务创建为待翻译: id={}", savedDocument.getId());
        return savedDocument;
    }

    /**
     * 获取文档翻译任务列表。
     *
     * @param status 任务状态（可选）
     * @param pageable 分页参数
     * @return 文档翻译任务分页列表
     * @throws Exception 查询过程中的异常
     */
    @Override
    public Page<DocumentTranslation> getDocumentTranslations(
            String status,
            Pageable pageable
    ) throws Exception {
        Long userId = UserContext.getUserId();
        log.info("获取文档翻译任务列表: userId={}, status={}", userId, status);

        ProcessingStatusEnum statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = ProcessingStatusEnum.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.warn("无效的状态参数: {}，忽略状态筛选", status);
            }
        }

        // 优先组合筛选（用户+状态）
        if (userId != null && statusEnum != null) {
            return documentTranslationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, statusEnum, pageable);
        }

        // 仅按用户筛选
        if (userId != null) {
            return documentTranslationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        // 仅按状态筛选
        if (statusEnum != null) {
            return documentTranslationRepository.findByStatusOrderByCreatedAtDesc(statusEnum, pageable);
        }

        // 无筛选，按控制器传入的倒序分页
        return documentTranslationRepository.findAll(pageable);
    }

    /**
     * 根据ID获取文档翻译任务详情。
     *
     * @param id 任务ID
     * @return 文档翻译任务详情，不存在则返回null
     * @throws Exception 查询过程中的异常
     */
    @Override
    public DocumentTranslation getDocumentTranslationById(Long id) throws Exception {

        log.info("获取文档翻译任务详情: id={}", id);

        return documentTranslationRepository.findById(id).orElse(null);
    }


    /**
     * 获取文档翻译进度。
     *
     * @param id 任务ID
     * @return 文档翻译进度响应
     * @throws Exception 查询过程中的异常
     */
    @Override
    public DocumentProgressResponse getTranslationProgressResponse(Long id) throws Exception {

        log.info("获取文档翻译进度响应: id={}", id);

        DocumentTranslation document = documentTranslationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档翻译任务不存在"));

        return DocumentProgressResponse.builder()
                .taskId(id)
                .status(document.getStatus().getValue())
                .progress(document.getProgress())
                .totalPages(10) // 模拟数据
                .processedPages(document.getProgress() / 10)
                .statusMessage(document.getStatusMessage())
                .processingTime(45000L) // 模拟数据
                .build();
    }

    /**
     * 下载翻译后的文档。
     *
     * @param id 任务ID
     * @return 文档字节数组，如果文档不存在或未完成则返回null
     * @throws Exception 下载过程中的异常
     */
    @Override
    public byte[] downloadTranslatedDocument(Long id) throws Exception {
        log.info("下载翻译后的文档: id={}", id);

        DocumentTranslation document = documentTranslationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档翻译任务不存在"));

        if (document.getStatus() != ProcessingStatusEnum.COMPLETED) {
            throw new IllegalStateException("文档翻译未完成，无法下载");
        }

        String objectName = document.getTranslatedFilePath();
        if (objectName == null || objectName.isEmpty()) {
            throw new IllegalStateException("译文文件未生成或路径为空");
        }

        // 从存储下载字节内容
        byte[] bytes = storageService.downloadBytes(objectName);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("译文字节为空，下载失败");
        }
        return bytes;
    }

    /**
     * 取消文档翻译任务。
     *
     * @param id 任务ID
     * @return 是否成功取消
     * @throws Exception 取消过程中的异常
     */
    @Override
    public boolean cancelTranslation(Long id) throws Exception {

        log.info("取消文档翻译任务: id={}", id);

        DocumentTranslation document = documentTranslationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档翻译任务不存在"));

        if (document.getStatus() == ProcessingStatusEnum.COMPLETED) {
            throw new IllegalStateException("文档翻译已完成，无法取消");
        }

        document.setStatus(ProcessingStatusEnum.CANCELLED);
        document.setStatusMessage("任务已取消");
        document.setUpdatedAt(LocalDateTime.now());

        documentTranslationRepository.save(document);

        return true;
    }

    /**
     * 重新翻译文档。
     *
     * @param id 原任务ID
     * @param engine 新的翻译引擎（可选）
     * @return 新的文档翻译任务
     * @throws Exception 重新翻译过程中的异常
     */
    @Override
    public DocumentTranslation retranslateDocument(Long id, String engine) throws Exception {

        log.info("重新翻译文档: id={}, engine={}", id, engine);

        DocumentTranslation originalDocument = documentTranslationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("原文档翻译任务不存在"));

        // 创建新的翻译任务
        DocumentTranslation newDocument = DocumentTranslation.builder()
                .originalFilename(originalDocument.getOriginalFilename())
                .fileSize(originalDocument.getFileSize())
                .fileType(originalDocument.getFileType())
                .sourceLanguage(originalDocument.getSourceLanguage())
                .targetLanguage(originalDocument.getTargetLanguage())
                .translationType(originalDocument.getTranslationType())
                .translationEngine(engine != null ? engine : originalDocument.getTranslationEngine())
                .useTerminology(originalDocument.getUseTerminology())
                .userId(originalDocument.getUserId())
                .priority(originalDocument.getPriority())
                .status(ProcessingStatusEnum.PENDING)
                .progress(0)
                .statusMessage("重新翻译任务已创建")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return documentTranslationRepository.save(newDocument);
    }

    /**
     * 开始处理已上传的文档翻译任务（支持覆盖目标语言与翻译模式）。
     *
     * @param id 任务ID
     * @param engine 翻译引擎（可选）
     * @param targetLanguage 目标语言（可选）
     * @param translationType 翻译模式（可选）
     * @return 更新后的文档翻译任务
     * @throws Exception 处理过程中的异常
     */
    @Override
    public DocumentTranslation startTranslation(Long id, String engine, String targetLanguage, String translationType) throws Exception {
        log.info("开始处理文档翻译任务: id={}, engine={}", id, engine);

        DocumentTranslation task = documentTranslationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档翻译任务不存在"));

        // 基本校验
        if (task.getSourceFilePath() == null || task.getSourceFilePath().isEmpty()) {
            throw new IllegalStateException("源文件路径不存在，无法开始翻译");
        }

        long startTime = System.currentTimeMillis();

        boolean wasProcessing = task.getStatus() == ProcessingStatusEnum.PROCESSING;
        // 若任务正在处理中，则触发安全重启：重置进度与状态消息
        if (wasProcessing) {
            log.info("检测到任务正在处理中，执行安全重启: taskId={}", id);
            task.setProgress(0);
            task.setProcessedPages(0);
            task.setTranslatedFilePath(null);
            task.setTranslatedContent(null);
            task.setStatusMessage("用户重新开始翻译，已重置进度");
        }

        // 更新任务为处理中
        task.setStatus(ProcessingStatusEnum.PROCESSING);
        task.setProgress(10);
        task.setStatusMessage("开始处理文档翻译");
        task.setTranslationEngine(engine != null ? engine : task.getTranslationEngine());
        if (targetLanguage != null && !targetLanguage.isBlank()) {
            task.setTargetLanguage(targetLanguage);
        }
        if (translationType != null && !translationType.isBlank()) {
            try {
                task.setTranslationType(TranslationTypeEnum.valueOf(translationType));
            } catch (IllegalArgumentException ex) {
                log.warn("无效的翻译模式: {}，沿用原值: {}", translationType, task.getTranslationType());
            }
        }
        // 在开始翻译前进行固定扣点（10点），点数优先、会员补足
        // 避免对已经在处理中的任务重复扣点，仅在非处理中状态切换到处理中时扣点
        if (!wasProcessing) {
            Long ctxUserId = UserContext.getUserId();
            Long effectiveUserId = (task.getUserId() != null) ? task.getUserId() : ctxUserId;
            // 兜底：从当前请求上下文解析用户ID（X-User-Id / 请求参数）
            if (effectiveUserId == null) {
                try {
                    String uidStr = UserContextUtils.getCurrentValidUserId();
                    if (uidStr != null && !uidStr.isBlank()) {
                        effectiveUserId = Long.valueOf(uidStr.trim());
                    }
                } catch (Exception ignore) {
                }
            }
            if (effectiveUserId != null) {
                task.setUserId(effectiveUserId);
                try {
                    pointsService.ensureAccount(effectiveUserId);
                } catch (Exception ignore) {
                }
                long requiredPoints = pointsProperties.getDocumentDeduction();
                log.info("准备扣点: userId={}, requiredPoints={}, taskId={}", effectiveUserId, requiredPoints, task.getId());
                performDeduction(effectiveUserId, requiredPoints, String.valueOf(task.getId()));
                String msg = task.getStatusMessage();
                task.setStatusMessage((msg == null || msg.isBlank() ? "" : (msg + "，")) + "已扣点：" + requiredPoints);
            } else {
                log.warn("开始翻译时缺少用户ID，跳过扣点: taskId={}", task.getId());
            }
        }
        task.setStartTime(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setStatus(ProcessingStatusEnum.PROCESSING);
        task.setStatusMessage("任务已启动，正在处理中");
        if (task.getProgress() == null || task.getProgress() < 1) {
            task.setProgress(1);
        }
        documentTranslationRepository.save(task);

        // 推进到 20%：准备阶段
        updateProgress(task.getId(), 20, "准备翻译环境");

        // 递增运行版本号，用于让旧的处理线程主动退出
        int newVersion = taskRunVersion.getOrDefault(id, 0) + 1;
        taskRunVersion.put(id, newVersion);

        // 生成译文对象名
        String ext = getFileExtension(task.getOriginalFilename());
        String uuid = UUID.randomUUID().toString();
        String translatedObject = "translated/" + uuid + "-" + task.getTargetLanguage() +
                (ext != null && !ext.isEmpty() ? ("." + ext) : "");

        // 异步处理翻译与进度
        final Long taskId = task.getId();
        final int expectedVersion = newVersion;
        new Thread(() -> {
            try {
                DocumentTranslation t0 = documentTranslationRepository.findById(taskId).orElse(null);
                if (t0 == null) return;
                // 如果在启动时已经被重启，则不继续执行
                if (isStaleRun(taskId, expectedVersion)) {
                    log.info("检测到版本号不匹配，终止旧任务线程: taskId={}, expectedVersion={}", taskId, expectedVersion);
                    return;
                }
                log.info("开始具体翻译处理: taskId={}, file={}, type={}, srcLang={}, tgtLang={}, engine={}",
                        taskId, t0.getOriginalFilename(), t0.getFileType(), t0.getSourceLanguage(), t0.getTargetLanguage(), t0.getTranslationEngine());

                // 下载源文件
                byte[] sourceBytes = storageService.downloadBytes(t0.getSourceFilePath());
                if (sourceBytes == null || sourceBytes.length == 0) {
                    throw new IllegalStateException("源文件内容为空");
                }

                byte[] translatedBytes;
                String lowerExt = (ext == null ? "" : ext.toLowerCase());

                // 使用工厂选择翻译器
                DocumentFileTranslator translator =
                        DocumentFileTranslatorFactory.forExtension(lowerExt);

                // 推进到 40%：已下载源文件
                updateProgress(taskId, 40, "已下载源文件，解析中");

                // 推进到 60%：开始处理内容
                updateProgress(taskId, 60, "已下载源文件，开始处理内容");
                translatedBytes = translator.translate(
                        sourceBytes,
                        t0,
                        translationService,
                        (percent, message) -> {
                            // 仅当未重启时才更新进度，避免旧线程覆盖新任务进度
                            if (!isStaleRun(taskId, expectedVersion)) {
                                updateProgress(taskId, percent, message);
                            }
                        }
                );
                if (isStaleRun(taskId, expectedVersion)) {
                    log.info("译文生成后检测到重启，终止上传流程: taskId={}, expectedVersion={}", taskId, expectedVersion);
                    return;
                }

                // 推进进度到75%、90%
                updateProgress(taskId, 75, "整合译文中");
                updateProgress(taskId, 90, "内容处理完成，准备上传");

                if (isStaleRun(taskId, expectedVersion)) {
                    log.info("上传前检测到重启，终止旧任务线程: taskId={}, expectedVersion={}", taskId, expectedVersion);
                    return;
                }

                // 上传译文
                String mimeType = getMimeType(ext);
                storageService.uploadBytes(translatedBytes, mimeType, translatedObject);
                log.info("译文已上传存储: object={}, mimeType={}, size={}", translatedObject, mimeType, translatedBytes.length);

                DocumentTranslation t = documentTranslationRepository.findById(taskId).orElse(null);
                if (t == null) return;
                t.setTranslatedFilePath(translatedObject);
                t.setTranslatedContent(translatedBytes);
                t.setProgress(100);
                t.setStatus(ProcessingStatusEnum.COMPLETED);
                t.setStatusMessage("文档翻译完成，可下载");
                t.setProcessingTime(System.currentTimeMillis() - startTime);
                t.setCompletionTime(LocalDateTime.now());
                t.setCompletedAt(LocalDateTime.now());
                t.setUpdatedAt(LocalDateTime.now());
                documentTranslationRepository.save(t);
                log.info("任务完成: taskId={}, 用时={}ms", taskId, (System.currentTimeMillis() - startTime));
            } catch (Exception ex) {
                log.error("翻译任务执行失败: id={}", taskId, ex);
                DocumentTranslation t = documentTranslationRepository.findById(taskId).orElse(null);
                if (t != null) {
                    // 异常退出时改为暂停状态，便于后续恢复/继续
                    t.setStatus(ProcessingStatusEnum.PAUSED);
                    t.setStatusMessage("任务异常退出，已暂停: " + ex.getMessage());
                    t.setUpdatedAt(LocalDateTime.now());
                    documentTranslationRepository.save(t);
                }
            }
        }).start();

        return task;
    }

    /**
     * 将进度更新到指定百分比并记录日志。
     *
     * @param taskId 任务ID
     * @param progress 进度百分比
     * @param message 进度消息
     */
    private void updateProgress(Long taskId, int progress, String message) {
        try {
            DocumentTranslation t = documentTranslationRepository.findById(taskId).orElse(null);
            if (t == null) return;
            int current = t.getProgress() == null ? 0 : t.getProgress();
            // 保证进度单调不降，避免旧线程或回调覆盖更高进度
            int next = Math.max(current, progress);
            t.setProgress(next);
            t.setStatusMessage(message);
            if (progress < 100 && t.getStatus() != ProcessingStatusEnum.PROCESSING) {
                t.setStatus(ProcessingStatusEnum.PROCESSING);
            }
            t.setUpdatedAt(LocalDateTime.now());
            documentTranslationRepository.save(t);
            log.info("任务进度: taskId={}, progress={}%, message={}", taskId, next, message);
        } catch (Exception e) {
            log.warn("更新进度失败: taskId={}, progress={}, error={}", taskId, progress, e.getMessage());
        }
    }
    

    /**
     * 检查当前线程是否已经过期（被新的“开始翻译”调用重启）。
     *
     * @param taskId 任务ID
     * @param expectedVersion 期望的版本号
     * @return 如果当前线程已过期，则返回 true
     */
    private boolean isStaleRun(Long taskId, int expectedVersion) {
        Integer current = taskRunVersion.get(taskId);
        return current == null || !Objects.equals(current, expectedVersion);
    }

    /**
     * 调用翻译服务翻译字符串，失败时回退到简单标记以避免中文原样输出。
     *
     * @param text 要翻译的文本
     * @param sourceLang 源语言
     * @param targetLang 目标语言
     * @param engine 翻译引擎
     * @return 翻译后的文本
     */
    private String translateString(String text, String sourceLang, String targetLang, String engine) {
        try {
            TranslationRequest req = TranslationRequest.builder()
                    .sourceText(text)
                    .sourceLanguage(sourceLang == null ? "auto" : sourceLang)
                    .targetLanguage(targetLang == null ? "en" : targetLang)
                    .translationType("TEXT")
                    .translationEngine(engine == null ? "ALIBABA_CLOUD" : engine)
                    .useTerminology(true)
                    .useRag(true)
                    .build();
            TranslationResponse resp = translationService.translate(req);
            if (resp != null && resp.getTranslatedText() != null) {
                return resp.getTranslatedText();
            }
        } catch (Exception e) {
            log.error("文本翻译失败，回退到标记: {}", e.getMessage());
        }
        // 回退：保留原文但不添加语言标签，避免出现 [en]
        return text;
    }

    /**
     * 删除文档翻译任务。
     *
     * @param id 任务ID
     * @return 是否成功删除
     * @throws Exception 删除过程中的异常
     */
    @Override
    public boolean deleteDocumentTranslation(Long id) throws Exception {

        log.info("删除文档翻译任务: id={}", id);

        if (!documentTranslationRepository.existsById(id)) {
            throw new IllegalArgumentException("文档翻译任务不存在");
        }

        documentTranslationRepository.deleteById(id);

        return true;
    }


    /**
     * 获取支持的文档类型。
     *
     * @return 支持的文档类型列表
     * @throws Exception 获取过程中的异常
     */
    @Override
    public List<DocumentTypeResponse> getSupportedDocumentTypesResponse() throws Exception {

        log.info("获取支持的文档类型响应");

        List<DocumentTypeResponse> types = new ArrayList<>();
        for (Map.Entry<String, String> entry : SUPPORTED_DOCUMENT_TYPES.entrySet()) {
            types.add(DocumentTypeResponse.builder()
                    .typeCode(entry.getKey())
                    .typeName(entry.getValue())
                    .fileExtension(entry.getKey())
                    .mimeType(getMimeType(entry.getKey()))
                    .maxFileSize(50 * 1024 * 1024L) // 50MB
                    .isSupported(true)
                    .description(entry.getValue())
                    .build());
        }

        return types;
    }

    /**
     * 获取文档翻译统计信息。
     *
     * @param userId 用户ID
     * @return 文档翻译统计信息
     * @throws Exception 获取过程中的异常
     */
    @Override
    public DocumentTranslationStatisticsDTO getDocumentTranslationStatistics(Long userId) throws Exception {

        log.info("获取文档翻译统计信息: userId={}", userId);
        // 示例：返回一个包含统计信息的DTO
        return new DocumentTranslationStatisticsDTO(100L, 80L, 5L, 512.5, 15000L, "PDF");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DocumentStatisticsResponse getDocumentTranslationStatisticsResponse(Long userId) throws Exception {

        log.info("获取文档翻译统计信息响应: userId={}", userId);

        return DocumentStatisticsResponse.builder()
                .totalDocuments(100L)
                .totalFileSize(1024L * 1024 * 1024) // 1GB
                .averageProcessingTime(30000L)
                .todayDocuments(10L)
                .weekDocuments(50L)
                .monthDocuments(100L)
                .averageQualityScore(0.85)
                .totalDownloads(80L)
                .startTime(LocalDateTime.now().minusDays(30))
                .endTime(LocalDateTime.now())
                .build();
    }

    /**
     * 批量删除文档翻译任务。
     *
     * @param ids 任务ID列表
     * @return 成功删除的数量
     * @throws Exception 删除过程中的异常
     */
    @Override
    public int batchDeleteDocumentTranslations(List<Long> ids) throws Exception {

        log.info("批量删除文档翻译任务: ids={}", ids);

        int deletedCount = 0;
        for (Long id : ids) {
            if (documentTranslationRepository.existsById(id)) {
                documentTranslationRepository.deleteById(id);
                deletedCount++;
            }
        }

        return deletedCount;
    }

    /**
     * 搜索文档翻译任务。
     *
     * @param keyword 关键词
     * @param fileType 文件类型
     * @param status 状态
     * @param pageable 分页信息
     * @return 文档翻译任务分页列表
     * @throws Exception 搜索过程中的异常
     */
    @Override
    public Page<DocumentTranslation> searchDocumentTranslations(
            String keyword,
            String fileType,
            String status,
            Pageable pageable
    ) throws Exception {
        Long userId = UserContext.getUserId();
        log.info("搜索文档翻译任务: keyword={}, userId={}, fileType={}, status={}",
                keyword, userId, fileType, status);

        // 这里应该实现搜索逻辑，现在返回空页面
        return documentTranslationRepository.findAll(pageable);
    }

    /**
     * 获取文档翻译质量评估。
     *
     * @param id 任务ID
     * @return 质量评估响应
     */
    @Override
    public QualityAssessmentResponse getDocumentQualityAssessment(Long id) {
        log.info("获取文档翻译质量评估: id={}", id);

        return qualityAssessmentRepository.findById(id).map(x -> {
            QualityAssessmentResponse qualityAssessmentResponse = new QualityAssessmentResponse();
            qualityAssessmentResponse.setOverallScore(x.getOverallScore());
            qualityAssessmentResponse.setAccuracyScore(x.getAccuracyScore());
            qualityAssessmentResponse.setFluencyScore(x.getFluencyScore());
            qualityAssessmentResponse.setConsistencyScore(x.getConsistencyScore());
            qualityAssessmentResponse.setImprovementSuggestions(Arrays.asList(x.getImprovementSuggestions()));
            return qualityAssessmentResponse;
        }).orElse(new QualityAssessmentResponse());
    }

    /**
     * 检查文档格式是否支持。
     *
     * @param filename 文件名
     * @param contentType 内容类型
     * @return 是否支持
     */
    @Override
    public boolean isDocumentFormatSupported(String filename, String contentType) {

        if (filename == null) {
            return false;
        }

        String extension = getFileExtension(filename).toLowerCase();
        return SUPPORTED_DOCUMENT_TYPES.containsKey(extension);
    }

    /**
     * 获取翻译预估信息。
     *
     * @param file 文件
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 翻译预估信息
     * @throws Exception 获取过程中的异常
     */
    @Override
    public TranslationEstimateDTO getTranslationEstimate(
            MultipartFile file,
            String sourceLanguage,
            String targetLanguage
    ) throws Exception {

        log.info("获取文档翻译预估信息: filename={}, sourceLanguage={}, targetLanguage={}",
                file.getOriginalFilename(), sourceLanguage, targetLanguage);
        return new TranslationEstimateDTO(file.getSize() / 2, 30, 0.0, true);
    }

    /**
     * 估算文档翻译所需点数：按近似字符数每千字1点数。
     *
     * @param fileSizeBytes 文件大小（以字节为单位）
     * @return 估算的所需点数
     */
    private long estimateRequiredPointsForFile(long fileSizeBytes) {
        // 文件翻译固定消耗点数（配置化）
        return pointsProperties.getDocumentDeduction();
    }

    /**
     * 执行扣点：固定扣减 requiredPoints 点数；不足则抛异常，不使用会员配额。
     *
     * @param userId 用户ID
     * @param requiredPoints 需要扣除的点数
     * @param referenceId 引用ID
     */
    private void performDeduction(Long userId, long requiredPoints, String referenceId) {
        log.info("扣点计算开始(强制点数): userId={}, requiredPoints={}, referenceId={}", userId, requiredPoints, referenceId);
        // 幂等保护：若已存在同任务的扣点记录（-requiredPoints），则跳过重复扣点
        try {
            boolean alreadyDeducted = pointsTransactionRepository
                    .existsByUserIdAndReferenceIdAndTypeAndDelta(userId, referenceId,
                            TransactionTypeEnum.DEDUCT, -requiredPoints);
            if (alreadyDeducted) {
                log.info("检测到已存在扣点交易，跳过重复扣点: userId={}, referenceId={}, points={}", userId, referenceId, requiredPoints);
                return;
            }
        } catch (Exception ignore) {
        }

        long balance = pointsService.getBalance(userId);
        log.info("当前点数余额: balance={}", balance);
        if (balance < requiredPoints) {
            log.warn("点数不足，无法扣减: userId={}, required={}, balance={}, referenceId={}",
                    userId, requiredPoints, balance, referenceId);
            throw new InsufficientPointsException("点数余额不足，文件翻译需要扣减 " + requiredPoints + " 点");
        }

        pointsService.deduct(userId, requiredPoints, "文件翻译固定扣点", referenceId);
        log.info("文件翻译扣点完成: userId={}, deducted={}, referenceId={}", userId, requiredPoints, referenceId);
    }

    /**
     * 更新翻译状态。
     *
     * @param id 任务ID
     * @param status 状态
     * @param progress 进度
     * @param message 消息
     * @return 是否成功更新
     * @throws Exception 更新过程中的异常
     */
    @Override
    public boolean updateTranslationStatus(Long id, String status, Integer progress, String message) throws Exception {

        log.info("更新文档翻译任务状态: id={}, status={}, progress={}, message={}",
                id, status, progress, message);

        DocumentTranslation document = documentTranslationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文档翻译任务不存在"));

        document.setStatus(ProcessingStatusEnum.valueOf(status));
        if (progress != null) {
            document.setProgress(progress);
        }
        if (message != null) {
            document.setStatusMessage(message);
        }
        document.setUpdatedAt(LocalDateTime.now());

        documentTranslationRepository.save(document);

        return true;
    }

    /**
     * 获取用户文档翻译配额。
     *
     * @return 用户文档翻译配额
     * @throws Exception 获取过程中的异常
     */
    @Override
    public Map<String, Object> getUserDocumentQuota() throws Exception {
        Long userId = UserContext.getUserId();
        log.info("获取用户文档翻译配额: userId={}", userId);

        Map<String, Object> quota = new HashMap<>();
        quota.put("usedQuota", 50);
        quota.put("remainingQuota", 50);
        quota.put("quotaLimit", 100);
        quota.put("resetDate", LocalDateTime.now().plusDays(30));

        return quota;
    }

    /**
     * 清理过期的翻译任务。
     *
     * @param daysToKeep 保留天数
     * @return 清理的记录数
     * @throws Exception 清理过程中的异常
     */
    @Override
    public long cleanupExpiredTranslations(int daysToKeep) throws Exception {

        log.info("清理过期的文档翻译任务: daysToKeep={}", daysToKeep);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        // 模拟查找过期的文档
        List<DocumentTranslation> expiredDocuments = new ArrayList<>();

        int cleanedCount = expiredDocuments.size();
        documentTranslationRepository.deleteAll(expiredDocuments);

        return cleanedCount;
    }

    /**
     * 获取文件扩展名。
     *
     * @param filename 文件名
     * @return 文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 根据文件类型获取MIME类型。
     *
     * @param fileType 文件类型
     * @return MIME类型
     */
    private String getMimeType(String fileType) {
        switch (fileType.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt":
                return "text/plain";
            case "html":
                return "text/html";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 根据文件类型获取图标类名。
     *
     * @param fileType 文件类型
     * @return 图标类名
     */
    private String getIconClass(String fileType) {
        switch (fileType.toLowerCase()) {
            case "pdf":
                return "fas fa-file-pdf";
            case "doc":
            case "docx":
                return "fas fa-file-word";
            case "xls":
            case "xlsx":
                return "fas fa-file-excel";
            case "ppt":
            case "pptx":
                return "fas fa-file-powerpoint";
            case "txt":
                return "fas fa-file-alt";
            case "html":
                return "fas fa-file-code";
            default:
                return "fas fa-file";
        }
    }
}