-- =====================================================
-- 智能翻译助手 AI Agent 数据库索引优化脚本
-- 创建时间: 2024-01-15
-- 版本: 1.0.0
-- =====================================================

-- 设置字符集
SET NAMES utf8mb4;

-- =====================================================
-- 1. 用户相关表索引优化
-- =====================================================

-- 用户表索引
ALTER TABLE `users` ADD INDEX `idx_status_role` (`status`, `role`);
ALTER TABLE `users` ADD INDEX `idx_last_login_time` (`last_login_time`);
ALTER TABLE `users` ADD INDEX `idx_created_at_status` (`created_at`, `status`);

-- 用户配置表索引
ALTER TABLE `user_settings` ADD INDEX `idx_setting_type` (`setting_type`);

-- =====================================================
-- 2. 翻译记录相关表索引优化
-- =====================================================

-- 翻译记录表索引
ALTER TABLE `translation_records` ADD INDEX `idx_user_created_at` (`user_id`, `created_at`);
ALTER TABLE `translation_records` ADD INDEX `idx_languages_type` (`source_language`, `target_language`, `translation_type`);
ALTER TABLE `translation_records` ADD INDEX `idx_engine_status` (`translation_engine`, `status`);
ALTER TABLE `translation_records` ADD INDEX `idx_quality_score` (`quality_score`);
ALTER TABLE `translation_records` ADD INDEX `idx_character_count` (`character_count`);
ALTER TABLE `translation_records` ADD INDEX `idx_processing_time` (`processing_time`);

-- 聊天会话表索引
ALTER TABLE `chat_sessions` ADD INDEX `idx_users_status` (`user_a_id`, `user_b_id`, `status`);
ALTER TABLE `chat_sessions` ADD INDEX `idx_languages` (`user_a_language`, `user_b_language`);
ALTER TABLE `chat_sessions` ADD INDEX `idx_last_active_at` (`last_active_at`);
ALTER TABLE `chat_sessions` ADD INDEX `idx_message_count` (`message_count`);

-- 聊天消息表索引
ALTER TABLE `chat_messages` ADD INDEX `idx_session_sequence` (`session_id`, `message_sequence`);
ALTER TABLE `chat_messages` ADD INDEX `idx_sender_receiver` (`sender_id`, `receiver_id`);
ALTER TABLE `chat_messages` ADD INDEX `idx_languages_status` (`source_language`, `target_language`, `translation_status`);
ALTER TABLE `chat_messages` ADD INDEX `idx_message_type` (`message_type`);
ALTER TABLE `chat_messages` ADD INDEX `idx_is_read` (`is_read`);
ALTER TABLE `chat_messages` ADD INDEX `idx_created_at_session` (`created_at`, `session_id`);

-- 文档翻译表索引
ALTER TABLE `document_translations` ADD INDEX `idx_user_status` (`user_id`, `status`);
ALTER TABLE `document_translations` ADD INDEX `idx_file_type_status` (`file_type`, `status`);
ALTER TABLE `document_translations` ADD INDEX `idx_languages_type` (`source_language`, `target_language`, `translation_type`);
ALTER TABLE `document_translations` ADD INDEX `idx_progress_status` (`progress`, `status`);
ALTER TABLE `document_translations` ADD INDEX `idx_quality_score` (`quality_score`);
ALTER TABLE `document_translations` ADD INDEX `idx_file_size` (`file_size`);
ALTER TABLE `document_translations` ADD INDEX `idx_priority_status` (`priority`, `status`);
ALTER TABLE `document_translations` ADD INDEX `idx_download_count` (`download_count`);

-- =====================================================
-- 3. 术语库相关表索引优化
-- =====================================================

-- 术语库条目表索引
ALTER TABLE `terminology_entries` ADD INDEX `idx_category_active` (`category`, `is_active`);
ALTER TABLE `terminology_entries` ADD INDEX `idx_domain` (`domain`);
ALTER TABLE `terminology_entries` ADD INDEX `idx_created_by` (`created_by`);
ALTER TABLE `terminology_entries` ADD INDEX `idx_usage_count_active` (`usage_count`, `is_active`);
ALTER TABLE `terminology_entries` ADD INDEX `idx_updated_at` (`updated_at`);

-- 术语库分类表索引
ALTER TABLE `terminology_categories` ADD INDEX `idx_parent_sort` (`parent_id`, `sort_order`);

-- =====================================================
-- 4. 质量评估相关表索引优化
-- =====================================================

-- 质量评估表索引
ALTER TABLE `quality_assessments` ADD INDEX `idx_mode_score` (`assessment_mode`, `overall_score`);
ALTER TABLE `quality_assessments` ADD INDEX `idx_engine_created` (`assessment_engine`, `created_at`);
ALTER TABLE `quality_assessments` ADD INDEX `idx_manual_assessor` (`is_manual_assessment`, `assessor_id`);
ALTER TABLE `quality_assessments` ADD INDEX `idx_scores` (`accuracy_score`, `fluency_score`, `consistency_score`, `completeness_score`);

-- =====================================================
-- 5. 系统配置相关表索引优化
-- =====================================================

-- 系统配置表索引
ALTER TABLE `system_configs` ADD INDEX `idx_type_editable` (`config_type`, `is_editable`);

-- 翻译引擎配置表索引
ALTER TABLE `translation_engines` ADD INDEX `idx_active_priority` (`is_active`, `priority`);

-- 支持语言表索引
ALTER TABLE `supported_languages` ADD INDEX `idx_active_sort` (`is_active`, `sort_order`);

-- 使用统计表索引
ALTER TABLE `usage_statistics` ADD INDEX `idx_date_user` (`stat_date`, `user_id`);
ALTER TABLE `usage_statistics` ADD INDEX `idx_translation_count` (`translation_count`);
ALTER TABLE `usage_statistics` ADD INDEX `idx_character_count` (`character_count`);

-- 操作日志表索引
ALTER TABLE `operation_logs` ADD INDEX `idx_type_created` (`operation_type`, `created_at`);
ALTER TABLE `operation_logs` ADD INDEX `idx_status_time` (`response_status`, `response_time`);
ALTER TABLE `operation_logs` ADD INDEX `idx_url_method` (`request_url`, `request_method`);

-- =====================================================
-- 6. 文件存储相关表索引优化
-- =====================================================

-- 文件存储表索引
ALTER TABLE `file_storage` ADD INDEX `idx_type_size` (`file_type`, `file_size`);
ALTER TABLE `file_storage` ADD INDEX `idx_storage_public` (`storage_type`, `is_public`);
ALTER TABLE `file_storage` ADD INDEX `idx_expires_at` (`expires_at`);
ALTER TABLE `file_storage` ADD INDEX `idx_download_count` (`download_count`);

-- =====================================================
-- 7. 通知相关表索引优化
-- =====================================================

-- 系统通知表索引
ALTER TABLE `system_notifications` ADD INDEX `idx_type_priority` (`notification_type`, `priority`);
ALTER TABLE `system_notifications` ADD INDEX `idx_active_publish` (`is_active`, `publish_time`);
ALTER TABLE `system_notifications` ADD INDEX `idx_expire_time` (`expire_time`);

-- 用户通知表索引
ALTER TABLE `user_notifications` ADD INDEX `idx_user_read` (`user_id`, `is_read`);
ALTER TABLE `user_notifications` ADD INDEX `idx_type_read` (`notification_type`, `is_read`);
ALTER TABLE `user_notifications` ADD INDEX `idx_read_time` (`read_time`);

-- =====================================================
-- 8. 复合索引优化（针对常用查询场景）
-- =====================================================

-- 翻译记录复合索引
ALTER TABLE `translation_records` ADD INDEX `idx_user_languages_created` (`user_id`, `source_language`, `target_language`, `created_at`);
ALTER TABLE `translation_records` ADD INDEX `idx_type_engine_status` (`translation_type`, `translation_engine`, `status`);

-- 聊天消息复合索引
ALTER TABLE `chat_messages` ADD INDEX `idx_session_type_status` (`session_id`, `message_type`, `translation_status`);

-- 文档翻译复合索引
ALTER TABLE `document_translations` ADD INDEX `idx_user_type_status` (`user_id`, `file_type`, `status`);
ALTER TABLE `document_translations` ADD INDEX `idx_languages_engine` (`source_language`, `target_language`, `translation_engine`);

-- 术语库条目复合索引
ALTER TABLE `terminology_entries` ADD INDEX `idx_languages_category_active` (`source_language`, `target_language`, `category`, `is_active`);

-- 质量评估复合索引
ALTER TABLE `quality_assessments` ADD INDEX `idx_record_mode_score` (`translation_record_id`, `assessment_mode`, `overall_score`);

-- =====================================================
-- 12. 会员与支付相关索引
-- =====================================================

-- 会员订单表索引
ALTER TABLE `membership_orders` ADD INDEX `idx_user_type_status` (`user_id`, `membership_type`, `status`);
ALTER TABLE `membership_orders` ADD INDEX `idx_status_paid_at` (`status`, `paid_at`);
ALTER TABLE `membership_orders` ADD INDEX `idx_effective_dates` (`start_at`, `end_at`);

-- 支付记录表索引
ALTER TABLE `payment_records` ADD INDEX `idx_order_provider_status` (`order_id`, `provider`, `status`);
ALTER TABLE `payment_records` ADD INDEX `idx_status_paid_at` (`status`, `paid_at`);

-- =====================================================
-- 9. 全文索引（用于搜索功能）
-- =====================================================

-- 翻译记录全文索引
ALTER TABLE `translation_records` ADD FULLTEXT INDEX `ft_source_text` (`source_text`);
ALTER TABLE `translation_records` ADD FULLTEXT INDEX `ft_translated_text` (`translated_text`);

-- 聊天消息全文索引
ALTER TABLE `chat_messages` ADD FULLTEXT INDEX `ft_original_message` (`original_message`);
ALTER TABLE `chat_messages` ADD FULLTEXT INDEX `ft_translated_message` (`translated_message`);

-- 术语库条目全文索引
ALTER TABLE `terminology_entries` ADD FULLTEXT INDEX `ft_source_term` (`source_term`);
ALTER TABLE `terminology_entries` ADD FULLTEXT INDEX `ft_target_term` (`target_term`);
ALTER TABLE `terminology_entries` ADD FULLTEXT INDEX `ft_notes` (`notes`);

-- 系统通知全文索引
ALTER TABLE `system_notifications` ADD FULLTEXT INDEX `ft_title_content` (`title`, `content`);

-- =====================================================
-- 10. 分区表设置（针对大数据量表）
-- =====================================================

-- 翻译记录表按月分区（如果数据量很大）
-- ALTER TABLE `translation_records` PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
--     PARTITION p202401 VALUES LESS THAN (202402),
--     PARTITION p202402 VALUES LESS THAN (202403),
--     PARTITION p202403 VALUES LESS THAN (202404),
--     PARTITION p202404 VALUES LESS THAN (202405),
--     PARTITION p202405 VALUES LESS THAN (202406),
--     PARTITION p202406 VALUES LESS THAN (202407),
--     PARTITION p202407 VALUES LESS THAN (202408),
--     PARTITION p202408 VALUES LESS THAN (202409),
--     PARTITION p202409 VALUES LESS THAN (202410),
--     PARTITION p202410 VALUES LESS THAN (202411),
--     PARTITION p202411 VALUES LESS THAN (202412),
--     PARTITION p202412 VALUES LESS THAN (202501),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

-- 操作日志表按月分区
-- ALTER TABLE `operation_logs` PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
--     PARTITION p202401 VALUES LESS THAN (202402),
--     PARTITION p202402 VALUES LESS THAN (202403),
--     PARTITION p202403 VALUES LESS THAN (202404),
--     PARTITION p202404 VALUES LESS THAN (202405),
--     PARTITION p202405 VALUES LESS THAN (202406),
--     PARTITION p202406 VALUES LESS THAN (202407),
--     PARTITION p202407 VALUES LESS THAN (202408),
--     PARTITION p202408 VALUES LESS THAN (202409),
--     PARTITION p202409 VALUES LESS THAN (202410),
--     PARTITION p202410 VALUES LESS THAN (202411),
--     PARTITION p202411 VALUES LESS THAN (202412),
--     PARTITION p202412 VALUES LESS THAN (202501),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

-- =====================================================
-- 11. 视图创建（常用查询优化）
-- =====================================================

-- 用户翻译统计视图
CREATE OR REPLACE VIEW `v_user_translation_stats` AS
SELECT 
    u.id as user_id,
    u.username,
    u.nickname,
    COUNT(tr.id) as total_translations,
    SUM(tr.character_count) as total_characters,
    AVG(tr.quality_score) as avg_quality_score,
    COUNT(CASE WHEN tr.translation_type = 'TEXT' THEN 1 END) as text_translations,
    COUNT(CASE WHEN tr.translation_type = 'DOCUMENT' THEN 1 END) as document_translations,
    COUNT(CASE WHEN tr.translation_type = 'CHAT' THEN 1 END) as chat_translations,
    MAX(tr.created_at) as last_translation_time
FROM users u
LEFT JOIN translation_records tr ON u.id = tr.user_id
GROUP BY u.id, u.username, u.nickname;

-- 术语库使用统计视图
CREATE OR REPLACE VIEW `v_terminology_usage_stats` AS
SELECT 
    te.id as terminology_id,
    te.source_term,
    te.target_term,
    te.source_language,
    te.target_language,
    te.category,
    te.usage_count,
    te.created_by,
    u.username as created_by_username,
    te.created_at,
    te.updated_at
FROM terminology_entries te
LEFT JOIN users u ON te.created_by = u.id
WHERE te.is_active = 1
ORDER BY te.usage_count DESC, te.updated_at DESC;

-- 翻译质量统计视图
CREATE OR REPLACE VIEW `v_translation_quality_stats` AS
SELECT 
    tr.id as translation_id,
    tr.source_language,
    tr.target_language,
    tr.translation_engine,
    tr.quality_score,
    qa.overall_score,
    qa.accuracy_score,
    qa.fluency_score,
    qa.consistency_score,
    qa.completeness_score,
    qa.assessment_mode,
    tr.created_at
FROM translation_records tr
LEFT JOIN quality_assessments qa ON tr.id = qa.translation_record_id
WHERE tr.quality_score IS NOT NULL
ORDER BY tr.quality_score DESC, tr.created_at DESC;

-- 系统性能统计视图
CREATE OR REPLACE VIEW `v_system_performance_stats` AS
SELECT 
    DATE(tr.created_at) as stat_date,
    tr.translation_engine,
    COUNT(*) as translation_count,
    AVG(tr.processing_time) as avg_processing_time,
    AVG(tr.quality_score) as avg_quality_score,
    SUM(tr.character_count) as total_characters,
    COUNT(CASE WHEN tr.status = 'COMPLETED' THEN 1 END) as completed_count,
    COUNT(CASE WHEN tr.status = 'FAILED' THEN 1 END) as failed_count
FROM translation_records tr
WHERE tr.created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(tr.created_at), tr.translation_engine
ORDER BY stat_date DESC, translation_count DESC;

-- 提交事务
-- =========================
-- 点数与会员相关索引
-- =========================

-- 点数账户：用户ID快速检索
CREATE INDEX `idx_points_accounts_user` ON `points_accounts`(`user_id`);

-- 点数交易：用户+时间查询 & 业务引用查询
CREATE INDEX `idx_points_transactions_user_created` ON `points_transactions`(`user_id`, `created_at`);
CREATE INDEX `idx_points_transactions_reference` ON `points_transactions`(`reference_id`);

-- 用户会员：状态检索与有效期范围查询
CREATE INDEX `idx_user_memberships_user_status` ON `user_memberships`(`user_id`, `status`);
CREATE INDEX `idx_user_memberships_user_dates` ON `user_memberships`(`user_id`, `start_at`, `end_at`);

COMMIT;

