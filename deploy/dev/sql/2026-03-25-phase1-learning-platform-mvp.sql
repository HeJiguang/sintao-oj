-- Phase 1 MVP schema for the learning-platform pivot
-- Business database: bitoj_dev
-- This script is meant for the current local business schema.

USE bitoj_dev;

-- Align the user table with pure email-code login.
ALTER TABLE tb_user
    MODIFY COLUMN phone varchar(20) NULL COMMENT 'optional phone after the email-login pivot',
    MODIFY COLUMN email varchar(100) NULL COMMENT 'email used by email-code login';

-- Extend question metadata for AI-driven training.
-- Run once on the current local schema.
ALTER TABLE tb_question
    ADD COLUMN algorithm_tag varchar(100) NULL COMMENT 'primary algorithm tag for training' AFTER difficulty,
    ADD COLUMN knowledge_tags varchar(500) NULL COMMENT 'comma separated knowledge tags for training' AFTER algorithm_tag,
    ADD COLUMN estimated_minutes int NULL COMMENT 'estimated solving time in minutes' AFTER knowledge_tags,
    ADD COLUMN training_enabled tinyint NOT NULL DEFAULT 1 COMMENT 'whether the question can appear in training plans' AFTER estimated_minutes;

-- Long-lived user training profile.
CREATE TABLE IF NOT EXISTS tb_training_profile (
    profile_id bigint unsigned NOT NULL COMMENT 'training profile id',
    user_id bigint unsigned NOT NULL COMMENT 'user id',
    current_level varchar(32) DEFAULT NULL COMMENT 'current level',
    target_direction varchar(100) DEFAULT NULL COMMENT 'target direction',
    weak_points text COMMENT 'weak points summary',
    strong_points text COMMENT 'strong points summary',
    last_test_exam_id bigint unsigned DEFAULT NULL COMMENT 'last test id',
    last_plan_id bigint unsigned DEFAULT NULL COMMENT 'last plan id',
    status tinyint NOT NULL DEFAULT 1 COMMENT '0 disabled 1 enabled',
    create_by bigint unsigned NOT NULL COMMENT 'creator id',
    create_time datetime NOT NULL COMMENT 'create time',
    update_by bigint unsigned DEFAULT NULL COMMENT 'updater id',
    update_time datetime DEFAULT NULL COMMENT 'update time',
    PRIMARY KEY (profile_id),
    UNIQUE KEY uk_training_profile_user_id (user_id),
    KEY idx_training_profile_last_plan_id (last_plan_id),
    KEY idx_training_profile_last_test_exam_id (last_test_exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='training profile';

-- Generated training plan header.
CREATE TABLE IF NOT EXISTS tb_training_plan (
    plan_id bigint unsigned NOT NULL COMMENT 'training plan id',
    user_id bigint unsigned NOT NULL COMMENT 'user id',
    plan_title varchar(100) NOT NULL COMMENT 'plan title',
    plan_goal varchar(255) DEFAULT NULL COMMENT 'plan goal',
    source_type varchar(32) NOT NULL COMMENT 'plan source type',
    based_on_exam_id bigint unsigned DEFAULT NULL COMMENT 'source test id',
    plan_status tinyint NOT NULL DEFAULT 0 COMMENT '0 pending 1 active 2 done 3 expired',
    ai_summary text COMMENT 'ai summary',
    create_by bigint unsigned NOT NULL COMMENT 'creator id',
    create_time datetime NOT NULL COMMENT 'create time',
    update_by bigint unsigned DEFAULT NULL COMMENT 'updater id',
    update_time datetime DEFAULT NULL COMMENT 'update time',
    PRIMARY KEY (plan_id),
    KEY idx_training_plan_user_id (user_id),
    KEY idx_training_plan_status (plan_status),
    KEY idx_training_plan_exam_id (based_on_exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='training plan';

-- Executable tasks under one plan.
CREATE TABLE IF NOT EXISTS tb_training_task (
    task_id bigint unsigned NOT NULL COMMENT 'training task id',
    plan_id bigint unsigned NOT NULL COMMENT 'training plan id',
    user_id bigint unsigned NOT NULL COMMENT 'user id',
    task_type varchar(20) NOT NULL COMMENT 'question test or review',
    question_id bigint unsigned DEFAULT NULL COMMENT 'related question id',
    exam_id bigint unsigned DEFAULT NULL COMMENT 'related test id',
    title_snapshot varchar(100) NOT NULL COMMENT 'title snapshot',
    task_order int NOT NULL COMMENT 'execution order',
    task_status tinyint NOT NULL DEFAULT 0 COMMENT '0 pending 1 done 2 skipped',
    recommended_reason varchar(500) DEFAULT NULL COMMENT 'recommendation reason',
    knowledge_tags_snapshot varchar(500) DEFAULT NULL COMMENT 'knowledge tags snapshot',
    due_time datetime DEFAULT NULL COMMENT 'suggested due time',
    create_by bigint unsigned NOT NULL COMMENT 'creator id',
    create_time datetime NOT NULL COMMENT 'create time',
    update_by bigint unsigned DEFAULT NULL COMMENT 'updater id',
    update_time datetime DEFAULT NULL COMMENT 'update time',
    PRIMARY KEY (task_id),
    KEY idx_training_task_plan_id (plan_id),
    KEY idx_training_task_user_id (user_id),
    KEY idx_training_task_status (task_status),
    KEY idx_training_task_question_id (question_id),
    KEY idx_training_task_exam_id (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='training task';
