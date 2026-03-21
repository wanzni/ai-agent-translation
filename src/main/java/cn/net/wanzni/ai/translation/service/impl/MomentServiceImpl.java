package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.entity.Moment;
import cn.net.wanzni.ai.translation.repository.MomentRepository;
import cn.net.wanzni.ai.translation.service.MomentService;
import cn.net.wanzni.ai.translation.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentServiceImpl implements MomentService {

    private final MomentRepository momentRepository;
    private final StorageService storageService;

    /**
     * 分页查询动态列表。
     *
     * @param pageable 分页参数
     * @return 动态分页列表
     * @throws Exception 查询异常
     */
    @Override
    public Page<Moment> list(Pageable pageable) throws Exception {
        return momentRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * 创建新的动态。
     *
     * @param userId 用户ID
     * @param content 动态内容
     * @param image 动态图片
     * @return 创建的动态实体
     * @throws Exception 创建异常
     */
    @Override
    public Moment create(Long userId, String content, MultipartFile image) throws Exception {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("内容不能为空");
        }

        String imageObject = null;
        String imageMime = null;
        if (image != null && !image.isEmpty()) {
            String ext = extractExtension(image.getOriginalFilename());
            String datePath = LocalDate.now().toString();
            String objectName = String.format("moments/%s/%s.%s", datePath, UUID.randomUUID(), ext);
            imageObject = storageService.upload(image, objectName);
            imageMime = image.getContentType();
            log.info("朋友圈图片已上传: {}", imageObject);
        }

        Moment moment = Moment.builder()
                .userId(userId)
                .content(content)
                .imageObject(imageObject)
                .imageMime(imageMime)
                .build();
        return momentRepository.save(moment);
    }

    /**
     * 获取动态图片的URL。
     *
     * @param imageObject 图片对象名称
     * @return 图片的URL
     * @throws Exception 获取URL异常
     */
    @Override
    public String getImageUrl(String imageObject) throws Exception {
        if (imageObject == null || imageObject.isEmpty()) {
            return null;
        }
        return storageService.getPresignedGetUrl(imageObject, 3600);
    }

    /**
     * 从文件名中提取文件扩展名。
     *
     * @param filename 文件名
     * @return 文件扩展名
     */
    private String extractExtension(String filename) {
        if (filename == null) return "bin";
        int idx = filename.lastIndexOf('.');
        if (idx == -1 || idx == filename.length() - 1) return "bin";
        return filename.substring(idx + 1).toLowerCase();
    }
}