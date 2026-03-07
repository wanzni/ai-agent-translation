package cn.net.susan.ai.translation.service;

import cn.net.susan.ai.translation.entity.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * 动态服务接口，提供动态的发布、列表查询和图片获取功能。
 */
public interface MomentService {
    /**
     * 分页查询动态列表。
     *
     * @param pageable 分页参数
     * @return 动态分页列表
     * @throws Exception 查询异常
     */
    Page<Moment> list(Pageable pageable) throws Exception;

    /**
     * 创建新的动态。
     *
     * @param userId 用户ID
     * @param content 动态内容
     * @param image 动态图片
     * @return 创建的动态实体
     * @throws Exception 创建异常
     */
    Moment create(Long userId, String content, MultipartFile image) throws Exception;

    /**
     * 获取动态图片的URL。
     *
     * @param imageObject 图片对象名称
     * @return 图片的URL
     * @throws Exception 获取URL异常
     */
    String getImageUrl(String imageObject) throws Exception;
}