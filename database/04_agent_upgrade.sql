-- Agent workflow upgrade: Day 1 bootstrap
-- Execute after 01_create_tables.sql

SET NAMES utf8mb4;

CREATE TABLE `agent_tasks` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `task_no` varchar(64) NOT NULL COMMENT 'Task number',
    `user_id` bigint DEFAULT NULL COMMENT 'User id',
    `task_type` varchar(32) NOT NULL COMMENT 'TEXT or DOCUMENT',
    `biz_type` varchar(64) DEFAULT NULL COMMENT 'Business type',
    `biz_id` varchar(64) DEFAULT NULL COMMENT 'Business id',
    `source_language` varchar(16) NOT NULL COMMENT 'Source language',
    `target_language` varchar(16) NOT NULL COMMENT 'Target language',
    `domain` varchar(100) DEFAULT NULL COMMENT 'Domain',
    `source_text` longtext COMMENT 'Source text',
    `input_file_id` bigint DEFAULT NULL COMMENT 'Input file id',
    `status` varchar(32) NOT NULL COMMENT 'Task status',
    `current_step` varchar(64) DEFAULT NULL COMMENT 'Current workflow step',
    `selected_model` varchar(64) DEFAULT NULL COMMENT 'Selected model',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT 'Retry count',
    `need_human_review` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Need human review',
    `final_quality_score` int DEFAULT NULL COMMENT 'Final quality score',
    `final_response` longtext COMMENT 'Final response',
    `trace_id` varchar(64) DEFAULT NULL COMMENT 'Trace id',
    `error_message` text COMMENT 'Error message',
    `started_at` datetime DEFAULT NULL COMMENT 'Task start time',
    `completed_at` datetime DEFAULT NULL COMMENT 'Task completion time',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_tasks_task_no` (`task_no`),
    KEY `idx_agent_tasks_user_id` (`user_id`),
    KEY `idx_agent_tasks_status` (`status`),
    KEY `idx_agent_tasks_task_type` (`task_type`),
    KEY `idx_agent_tasks_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent task table';

CREATE TABLE `agent_task_steps` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `task_id` bigint NOT NULL COMMENT 'Agent task id',
    `step_no` int NOT NULL COMMENT 'Step number',
    `step_type` varchar(64) NOT NULL COMMENT 'Step type',
    `step_name` varchar(100) NOT NULL COMMENT 'Step name',
    `tool_name` varchar(64) DEFAULT NULL COMMENT 'Tool name',
    `model_name` varchar(64) DEFAULT NULL COMMENT 'Model name',
    `input_json` longtext COMMENT 'Input payload',
    `output_json` longtext COMMENT 'Output payload',
    `status` varchar(32) NOT NULL COMMENT 'Step status',
    `duration_ms` bigint DEFAULT NULL COMMENT 'Duration ms',
    `error_message` text COMMENT 'Error message',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    KEY `idx_agent_task_steps_task_id` (`task_id`),
    KEY `idx_agent_task_steps_step_no` (`step_no`),
    CONSTRAINT `fk_agent_task_steps_task_id` FOREIGN KEY (`task_id`) REFERENCES `agent_tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent task step table';

CREATE TABLE `translation_memory_entries` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `source_text` longtext NOT NULL COMMENT 'Source text',
    `target_text` longtext NOT NULL COMMENT 'Target text',
    `source_language` varchar(16) NOT NULL COMMENT 'Source language',
    `target_language` varchar(16) NOT NULL COMMENT 'Target language',
    `domain` varchar(100) DEFAULT NULL COMMENT 'Domain',
    `source_text_hash` varchar(64) DEFAULT NULL COMMENT 'Source text hash',
    `quality_score` int DEFAULT NULL COMMENT 'Quality score',
    `approved` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Approved by review',
    `hit_count` int NOT NULL DEFAULT 0 COMMENT 'Hit count',
    `created_from_task_id` bigint DEFAULT NULL COMMENT 'Source agent task id',
    `created_by` bigint DEFAULT NULL COMMENT 'Creator user id',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    KEY `idx_tm_lang_pair` (`source_language`, `target_language`),
    KEY `idx_tm_domain` (`domain`),
    KEY `idx_tm_created_from_task_id` (`created_from_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Translation memory table';

CREATE TABLE `review_tasks` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `agent_task_id` bigint NOT NULL COMMENT 'Agent task id',
    `biz_type` varchar(64) DEFAULT NULL COMMENT 'Business type',
    `biz_id` varchar(64) DEFAULT NULL COMMENT 'Business id',
    `reason_code` varchar(64) DEFAULT NULL COMMENT 'Reason code',
    `issue_summary` text COMMENT 'Issue summary',
    `suggested_text` longtext COMMENT 'Suggested text',
    `final_text` longtext COMMENT 'Final text',
    `review_status` varchar(32) NOT NULL COMMENT 'Review status',
    `reviewer_id` bigint DEFAULT NULL COMMENT 'Reviewer id',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    KEY `idx_review_tasks_agent_task_id` (`agent_task_id`),
    KEY `idx_review_tasks_review_status` (`review_status`),
    CONSTRAINT `fk_review_tasks_agent_task_id` FOREIGN KEY (`agent_task_id`) REFERENCES `agent_tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Human review queue table';

ALTER TABLE `translation_records`
    ADD COLUMN `agent_task_id` bigint DEFAULT NULL COMMENT 'Associated agent task id',
    ADD COLUMN `tm_hit_count` int NOT NULL DEFAULT 0 COMMENT 'Translation memory hit count',
    ADD COLUMN `tool_trace_json` longtext COMMENT 'Tool trace json',
    ADD COLUMN `route_model` varchar(64) DEFAULT NULL COMMENT 'Route model',
    ADD COLUMN `review_status` varchar(32) DEFAULT NULL COMMENT 'Review status';

ALTER TABLE `quality_assessments`
    ADD COLUMN `terminology_score` int DEFAULT NULL COMMENT 'Terminology score',
    ADD COLUMN `number_score` int DEFAULT NULL COMMENT 'Number score',
    ADD COLUMN `format_score` int DEFAULT NULL COMMENT 'Format score',
    ADD COLUMN `llm_judge_score` int DEFAULT NULL COMMENT 'LLM judge score',
    ADD COLUMN `needs_retry` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Needs retry',
    ADD COLUMN `needs_human_review` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Needs human review';
