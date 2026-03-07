-- =====================================================
-- 智能翻译助手 AI Agent 数据库表结构设计
-- 基于产品原型和现有实体类设计
-- 创建时间: 2024-01-15
-- 版本: 1.0.0
-- =====================================================

-- 设置字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. 用户管理相关表
-- =====================================================

-- 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `email` varchar(100) NOT NULL COMMENT '邮箱',
    `password_hash` varchar(255) NOT NULL COMMENT '密码哈希',
    `nickname` varchar(100) DEFAULT NULL COMMENT '昵称',
    `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `language_preference` varchar(10) DEFAULT 'zh' COMMENT '语言偏好',
    `timezone` varchar(50) DEFAULT 'Asia/Shanghai' COMMENT '时区',
    `status` enum('ACTIVE','INACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
    `role` enum('USER','ADMIN','SUPER_ADMIN') NOT NULL DEFAULT 'USER' COMMENT '用户角色',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` varchar(45) DEFAULT NULL COMMENT '最后登录IP',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户配置表
DROP TABLE IF EXISTS `user_settings`;
CREATE TABLE `user_settings` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `setting_key` varchar(100) NOT NULL COMMENT '配置键',
    `setting_value` text COMMENT '配置值',
    `setting_type` enum('STRING','NUMBER','BOOLEAN','JSON') NOT NULL DEFAULT 'STRING' COMMENT '配置类型',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_setting` (`user_id`, `setting_key`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_user_settings_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户配置表';

-- =====================================================
-- 2. 翻译记录相关表（基于现有实体类）
-- =====================================================

-- 翻译记录表
DROP TABLE IF EXISTS `translation_records`;
CREATE TABLE `translation_records` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID（支持匿名翻译）',
    `source_language` varchar(10) NOT NULL COMMENT '源语言代码',
    `target_language` varchar(10) NOT NULL COMMENT '目标语言代码',
    `source_text` text NOT NULL COMMENT '源文本内容',
    `translated_text` text NOT NULL COMMENT '翻译结果',
    `translation_type` enum('TEXT','DOCUMENT','CHAT') NOT NULL COMMENT '翻译类型',
    `translation_engine` varchar(50) DEFAULT NULL COMMENT '翻译引擎',
    `quality_score` int(11) DEFAULT NULL COMMENT '质量评分（0-100）',
    `processing_time` bigint(20) DEFAULT NULL COMMENT '翻译耗时（毫秒）',
    `character_count` int(11) DEFAULT NULL COMMENT '字符数统计',
    `use_terminology` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否使用术语库',
    `status` enum('PENDING','PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'COMPLETED' COMMENT '翻译状态',
    `error_message` text COMMENT '错误信息',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_languages` (`source_language`, `target_language`),
    KEY `idx_translation_type` (`translation_type`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译记录表';

-- 聊天会话表
DROP TABLE IF EXISTS `chat_sessions`;
CREATE TABLE `chat_sessions` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `session_id` varchar(100) NOT NULL COMMENT '会话唯一标识',
    `user_a_id` varchar(100) DEFAULT NULL COMMENT '用户A的ID',
    `user_b_id` varchar(100) DEFAULT NULL COMMENT '用户B的ID',
    `user_a_language` varchar(10) NOT NULL COMMENT '用户A的语言',
    `user_b_language` varchar(10) NOT NULL COMMENT '用户B的语言',
    `session_title` varchar(200) DEFAULT NULL COMMENT '会话标题',
    `status` enum('ACTIVE','INACTIVE','ENDED','ARCHIVED') NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态',
    `message_count` int(11) NOT NULL DEFAULT '0' COMMENT '消息总数',
    `auto_translate` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用自动翻译',
    `use_terminology` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否使用术语库',
    `last_active_at` datetime DEFAULT NULL COMMENT '最后活跃时间',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `ended_at` datetime DEFAULT NULL COMMENT '会话结束时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_user_a_id` (`user_a_id`),
    KEY `idx_user_b_id` (`user_b_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- 聊天消息表
DROP TABLE IF EXISTS `chat_messages`;
CREATE TABLE `chat_messages` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id` bigint(20) NOT NULL COMMENT '关联的会话ID',
    `sender_id` varchar(100) NOT NULL COMMENT '发送者ID',
    `receiver_id` varchar(100) DEFAULT NULL COMMENT '接收者ID',
    `original_message` text NOT NULL COMMENT '原始消息内容',
    `translated_message` text COMMENT '翻译后的消息内容',
    `source_language` varchar(10) NOT NULL COMMENT '源语言代码',
    `target_language` varchar(10) DEFAULT NULL COMMENT '目标语言代码',
    `message_type` enum('TEXT','IMAGE','FILE','VOICE','SYSTEM') NOT NULL DEFAULT 'TEXT' COMMENT '消息类型',
    `translation_status` enum('PENDING','TRANSLATING','COMPLETED','FAILED','SKIPPED') NOT NULL DEFAULT 'PENDING' COMMENT '翻译状态',
    `translation_engine` varchar(50) DEFAULT NULL COMMENT '翻译引擎',
    `translation_time` bigint(20) DEFAULT NULL COMMENT '翻译耗时（毫秒）',
    `use_terminology` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否使用术语库',
    `message_sequence` int(11) DEFAULT NULL COMMENT '消息序号',
    `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读',
    `error_message` text COMMENT '错误信息',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_translation_status` (`translation_status`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_chat_messages_session_id` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- 文档翻译表
DROP TABLE IF EXISTS `document_translations`;
CREATE TABLE `document_translations` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '文档翻译ID',
    `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
    `original_filename` varchar(255) NOT NULL COMMENT '原始文件名',
    `file_type` enum('PDF','DOCX','TXT','XLSX','PPTX','HTML','XML','JSON') NOT NULL COMMENT '文件类型',
    `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小（字节）',
    `source_file_path` varchar(500) NOT NULL COMMENT '源文件路径',
    `translated_file_path` varchar(500) DEFAULT NULL COMMENT '翻译后文件路径',
    `source_language` varchar(10) NOT NULL COMMENT '源语言代码',
    `target_language` varchar(10) NOT NULL COMMENT '目标语言代码',
    `status` enum('PENDING','UPLOADING','PROCESSING','COMPLETED','FAILED','CANCELLED') NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
    `progress` int(11) NOT NULL DEFAULT '0' COMMENT '翻译进度（0-100）',
    `total_pages` int(11) DEFAULT NULL COMMENT '总页数/段落数',
    `processed_pages` int(11) NOT NULL DEFAULT '0' COMMENT '已处理页数/段落数',
    `translation_engine` varchar(50) DEFAULT NULL COMMENT '翻译引擎',
    `use_terminology` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否使用术语库',
    `quality_score` int(11) DEFAULT NULL COMMENT '质量评分（0-100）',
    `processing_time` bigint(20) DEFAULT NULL COMMENT '处理耗时（毫秒）',
    `error_message` text COMMENT '错误信息',
    `download_count` int(11) NOT NULL DEFAULT '0' COMMENT '下载次数',
    `translated_content` longblob COMMENT '翻译内容（二进制）',
    `translation_type` enum('STANDARD','PROFESSIONAL','FAST','ACCURATE') DEFAULT NULL COMMENT '翻译类型',
    `priority` int(11) DEFAULT NULL COMMENT '优先级',
    `original_content` text COMMENT '原始内容',
    `estimated_completion_time` datetime DEFAULT NULL COMMENT '预计完成时间',
    `last_download_time` datetime DEFAULT NULL COMMENT '最后下载时间',
    `status_message` varchar(500) DEFAULT NULL COMMENT '状态消息',
    `character_count` bigint(20) DEFAULT NULL COMMENT '字符数统计',
    `completion_time` datetime DEFAULT NULL COMMENT '完成时间',
    `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
    `start_time` datetime DEFAULT NULL COMMENT '开始时间',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_status` (`status`),
    KEY `idx_languages` (`source_language`, `target_language`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档翻译表';

-- =====================================================
-- 3. 术语库管理相关表
-- =====================================================

-- 术语库条目表
DROP TABLE IF EXISTS `terminology_entries`;
CREATE TABLE `terminology_entries` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '术语ID',
    `source_term` varchar(500) NOT NULL COMMENT '源术语',
    `target_term` varchar(500) NOT NULL COMMENT '目标翻译',
    `source_language` varchar(10) NOT NULL COMMENT '源语言代码',
    `target_language` varchar(10) NOT NULL COMMENT '目标语言代码',
    `category` enum('TECHNOLOGY','BUSINESS','MEDICAL','LEGAL','FINANCE','EDUCATION','SCIENCE','GENERAL') NOT NULL COMMENT '术语分类',
    `domain` varchar(100) DEFAULT NULL COMMENT '领域标签',
    `notes` text COMMENT '备注信息',
    `usage_count` int(11) NOT NULL DEFAULT '0' COMMENT '使用频率',
    `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
    `created_by` varchar(100) DEFAULT NULL COMMENT '创建者ID',
    `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_terminology` (`source_term`, `source_language`, `target_language`),
    KEY `idx_languages` (`source_language`, `target_language`),
    KEY `idx_category` (`category`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_usage_count` (`usage_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='术语库条目表';

-- 术语库分类表
DROP TABLE IF EXISTS `terminology_categories`;
CREATE TABLE `terminology_categories` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `category_name` varchar(100) NOT NULL COMMENT '分类名称',
    `category_code` varchar(50) NOT NULL COMMENT '分类代码',
    `description` text COMMENT '分类描述',
    `parent_id` bigint(20) DEFAULT NULL COMMENT '父分类ID',
    `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '排序顺序',
    `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`category_code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='术语库分类表';

-- =====================================================
-- 4. 质量评估相关表
-- =====================================================

-- 质量评估表
DROP TABLE IF EXISTS `quality_assessments`;
CREATE TABLE `quality_assessments` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评估ID',
    `translation_record_id` bigint(20) NOT NULL COMMENT '关联的翻译记录ID',
    `assessment_mode` enum('AUTOMATIC','MANUAL','HYBRID') NOT NULL COMMENT '评估模式',
    `overall_score` int(11) NOT NULL COMMENT '整体质量评分（0-100）',
    `accuracy_score` int(11) NOT NULL COMMENT '准确性评分（0-100）',
    `fluency_score` int(11) NOT NULL COMMENT '流畅性评分（0-100）',
    `consistency_score` int(11) NOT NULL COMMENT '一致性评分（0-100）',
    `completeness_score` int(11) NOT NULL COMMENT '完整性评分（0-100）',
    `improvement_suggestions` text COMMENT '改进建议（JSON格式）',
    `attention_points` text COMMENT '注意事项（JSON格式）',
    `strengths` text COMMENT '优点总结（JSON格式）',
    `assessment_details` text COMMENT '评估详情（JSON格式）',
    `assessment_time` bigint(20) DEFAULT NULL COMMENT '评估耗时（毫秒）',
    `assessment_engine` varchar(50) DEFAULT NULL COMMENT '评估引擎',
    `is_manual_assessment` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否人工评估',
    `assessor_id` varchar(100) DEFAULT NULL COMMENT '评估者ID',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_translation_record_id` (`translation_record_id`),
    KEY `idx_assessment_mode` (`assessment_mode`),
    KEY `idx_overall_score` (`overall_score`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_quality_assessments_translation_record_id` FOREIGN KEY (`translation_record_id`) REFERENCES `translation_records` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质量评估表';

-- =====================================================
-- 5. 系统配置和统计相关表
-- =====================================================

-- 系统配置表
DROP TABLE IF EXISTS `system_configs`;
CREATE TABLE `system_configs` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key` varchar(100) NOT NULL COMMENT '配置键',
    `config_value` text COMMENT '配置值',
    `config_type` enum('STRING','NUMBER','BOOLEAN','JSON') NOT NULL DEFAULT 'STRING' COMMENT '配置类型',
    `description` varchar(500) DEFAULT NULL COMMENT '配置描述',
    `category` varchar(50) DEFAULT NULL COMMENT '配置分类',
    `is_editable` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可编辑',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 翻译引擎配置表
DROP TABLE IF EXISTS `translation_engines`;
CREATE TABLE `translation_engines` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '引擎ID',
    `engine_name` varchar(50) NOT NULL COMMENT '引擎名称',
    `engine_code` varchar(50) NOT NULL COMMENT '引擎代码',
    `api_endpoint` varchar(500) DEFAULT NULL COMMENT 'API端点',
    `api_key` varchar(255) DEFAULT NULL COMMENT 'API密钥',
    `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
    `priority` int(11) NOT NULL DEFAULT '0' COMMENT '优先级',
    `max_requests_per_minute` int(11) DEFAULT NULL COMMENT '每分钟最大请求数',
    `supported_languages` text COMMENT '支持的语言列表（JSON格式）',
    `config_params` text COMMENT '配置参数（JSON格式）',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_engine_code` (`engine_code`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译引擎配置表';

-- 语言支持表
DROP TABLE IF EXISTS `supported_languages`;
CREATE TABLE `supported_languages` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '语言ID',
    `language_code` varchar(10) NOT NULL COMMENT '语言代码',
    `language_name` varchar(100) NOT NULL COMMENT '语言名称',
    `native_name` varchar(100) DEFAULT NULL COMMENT '本地语言名称',
    `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
    `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '排序顺序',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_language_code` (`language_code`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支持语言表';

-- 使用统计表
DROP TABLE IF EXISTS `usage_statistics`;
CREATE TABLE `usage_statistics` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '统计ID',
    `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
    `stat_date` date NOT NULL COMMENT '统计日期',
    `translation_count` int(11) NOT NULL DEFAULT '0' COMMENT '翻译次数',
    `character_count` bigint(20) NOT NULL DEFAULT '0' COMMENT '字符数',
    `document_count` int(11) NOT NULL DEFAULT '0' COMMENT '文档翻译次数',
    `chat_message_count` int(11) NOT NULL DEFAULT '0' COMMENT '聊天消息数',
    `terminology_usage_count` int(11) NOT NULL DEFAULT '0' COMMENT '术语库使用次数',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `stat_date`),
    KEY `idx_stat_date` (`stat_date`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用统计表';

-- 操作日志表
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
    `operation_type` varchar(50) NOT NULL COMMENT '操作类型',
    `operation_description` varchar(500) DEFAULT NULL COMMENT '操作描述',
    `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
    `request_method` varchar(10) DEFAULT NULL COMMENT '请求方法',
    `request_params` text COMMENT '请求参数',
    `response_status` int(11) DEFAULT NULL COMMENT '响应状态码',
    `response_time` bigint(20) DEFAULT NULL COMMENT '响应时间（毫秒）',
    `ip_address` varchar(45) DEFAULT NULL COMMENT 'IP地址',
    `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =====================================================
-- 6. 文件存储相关表
-- =====================================================

-- 文件存储表
DROP TABLE IF EXISTS `file_storage`;
CREATE TABLE `file_storage` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `file_name` varchar(255) NOT NULL COMMENT '文件名',
    `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
    `file_path` varchar(500) NOT NULL COMMENT '文件路径',
    `file_size` bigint(20) NOT NULL COMMENT '文件大小（字节）',
    `file_type` varchar(100) DEFAULT NULL COMMENT '文件类型',
    `mime_type` varchar(100) DEFAULT NULL COMMENT 'MIME类型',
    `file_hash` varchar(64) DEFAULT NULL COMMENT '文件哈希值',
    `storage_type` enum('LOCAL','OSS','S3','MINIO') NOT NULL DEFAULT 'LOCAL' COMMENT '存储类型',
    `user_id` varchar(100) DEFAULT NULL COMMENT '用户ID',
    `is_public` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否公开',
    `download_count` int(11) NOT NULL DEFAULT '0' COMMENT '下载次数',
    `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_hash` (`file_hash`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_storage_type` (`storage_type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件存储表';

-- =====================================================
-- 7. 通知和消息相关表
-- =====================================================

-- 系统通知表
DROP TABLE IF EXISTS `system_notifications`;
CREATE TABLE `system_notifications` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `title` varchar(200) NOT NULL COMMENT '通知标题',
    `content` text NOT NULL COMMENT '通知内容',
    `notification_type` enum('SYSTEM','TRANSLATION','QUALITY','TERMINOLOGY') NOT NULL DEFAULT 'SYSTEM' COMMENT '通知类型',
    `priority` enum('LOW','NORMAL','HIGH','URGENT') NOT NULL DEFAULT 'NORMAL' COMMENT '优先级',
    `target_users` text COMMENT '目标用户（JSON格式）',
    `is_global` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否全局通知',
    `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
    `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
    `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_notification_type` (`notification_type`),
    KEY `idx_priority` (`priority`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- 用户通知表
DROP TABLE IF EXISTS `user_notifications`;
CREATE TABLE `user_notifications` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户通知ID',
    `user_id` varchar(100) NOT NULL COMMENT '用户ID',
    `notification_id` bigint(20) DEFAULT NULL COMMENT '系统通知ID',
    `title` varchar(200) NOT NULL COMMENT '通知标题',
    `content` text NOT NULL COMMENT '通知内容',
    `notification_type` enum('SYSTEM','TRANSLATION','QUALITY','TERMINOLOGY') NOT NULL DEFAULT 'SYSTEM' COMMENT '通知类型',
    `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读',
    `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_notification_id` (`notification_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_user_notifications_notification_id` FOREIGN KEY (`notification_id`) REFERENCES `system_notifications` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知表';

-- =========================
-- 新增：点数账户、点数交易、用户会员
-- =========================

-- 点数账户表
CREATE TABLE IF NOT EXISTS `points_accounts` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` VARCHAR(64) NOT NULL,
  `balance` BIGINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_points_accounts_user` (`user_id`),
  CONSTRAINT `fk_points_accounts_user`
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 点数交易表
CREATE TABLE IF NOT EXISTS `points_transactions` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` VARCHAR(64) NOT NULL,
  `type` VARCHAR(16) NOT NULL,
  `delta` BIGINT NOT NULL,
  `reference_id` VARCHAR(128),
  `reason` VARCHAR(255),
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_points_tx_user_created` (`user_id`, `created_at`),
  INDEX `idx_points_tx_reference` (`reference_id`),
  CONSTRAINT `fk_points_tx_user`
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户会员表
CREATE TABLE IF NOT EXISTS `user_memberships` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` VARCHAR(64) NOT NULL,
  `type` VARCHAR(16) NOT NULL,
  `status` VARCHAR(16) NOT NULL,
  `start_at` DATETIME NOT NULL,
  `end_at` DATETIME NOT NULL,
  `period_quota` BIGINT NOT NULL DEFAULT 0,
  `period_used` BIGINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_membership_user_status` (`user_id`, `status`),
  INDEX `idx_membership_user_dates` (`user_id`, `start_at`, `end_at`),
  CONSTRAINT `fk_user_memberships_user`
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 会员订单表
CREATE TABLE IF NOT EXISTS `membership_orders` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(100) NOT NULL,
  `membership_type` ENUM('MONTHLY','QUARTERLY','YEARLY') NOT NULL,
  `period_quota` BIGINT NOT NULL,
  `months` INT NOT NULL DEFAULT 1,
  `amount` DECIMAL(10,2) NOT NULL,
  `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY',
  `status` ENUM('PENDING','PAID','CANCELLED','REFUNDED') NOT NULL DEFAULT 'PENDING',
  `start_at` DATETIME DEFAULT NULL,
  `end_at` DATETIME DEFAULT NULL,
  `paid_at` DATETIME DEFAULT NULL,
  `payment_count` INT NOT NULL DEFAULT 0,
  `latest_payment_id` BIGINT DEFAULT NULL,
  `external_trade_no` VARCHAR(128) DEFAULT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_membership_order_no` (`order_no`),
  INDEX `idx_membership_orders_user_status` (`user_id`, `status`),
  INDEX `idx_membership_orders_user_created` (`user_id`, `created_at`),
  INDEX `idx_membership_orders_trade_no` (`external_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 支付记录表
CREATE TABLE IF NOT EXISTS `payment_records` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `payment_no` VARCHAR(64) NOT NULL,
  `provider` ENUM('ALIPAY','WECHAT','STRIPE','PAYPAL','OTHER') NOT NULL,
  `method` ENUM('ALIPAY_APP','ALIPAY_QR','WECHAT_APP','WECHAT_QR','CREDIT_CARD','BANK_TRANSFER','OTHER') NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `currency` VARCHAR(10) NOT NULL DEFAULT 'CNY',
  `status` ENUM('INITIATED','SUCCESS','FAILED','REFUNDED','CANCELLED') NOT NULL DEFAULT 'INITIATED',
  `transaction_no` VARCHAR(128) DEFAULT NULL,
  `paid_at` DATETIME DEFAULT NULL,
  `notify_payload` TEXT,
  `error_message` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  INDEX `idx_payment_order_status` (`order_id`, `status`),
  INDEX `idx_payment_provider_status` (`provider`, `status`),
  INDEX `idx_payment_transaction_no` (`transaction_no`),
  CONSTRAINT `fk_payment_records_order`
    FOREIGN KEY (`order_id`) REFERENCES `membership_orders`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
