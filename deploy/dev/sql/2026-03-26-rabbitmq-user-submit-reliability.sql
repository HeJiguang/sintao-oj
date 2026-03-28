USE bitoj_dev;

ALTER TABLE tb_user_submit
    ADD COLUMN request_id varchar(64) NULL COMMENT '异步判题请求id' AFTER submit_id,
    ADD COLUMN judge_status tinyint NOT NULL DEFAULT 1 COMMENT '异步判题状态 0等待 1成功 2死信 3投递失败' AFTER case_judge_res,
    ADD COLUMN retry_count int NOT NULL DEFAULT 0 COMMENT '重试次数快照' AFTER judge_status,
    ADD COLUMN last_error varchar(1000) NULL COMMENT '最后一次错误摘要' AFTER retry_count,
    ADD COLUMN finish_time datetime NULL COMMENT '终态完成时间' AFTER last_error;

ALTER TABLE tb_user_submit
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

UPDATE tb_user_submit
SET request_id = CONCAT('legacy-', submit_id)
WHERE request_id IS NULL
   OR request_id = '';

ALTER TABLE tb_user_submit
    MODIFY COLUMN request_id varchar(64) NOT NULL COMMENT '异步判题请求id';

ALTER TABLE tb_user_submit
    ADD UNIQUE KEY uk_request_id (request_id),
    ADD KEY idx_user_question_exam_create (user_id, question_id, exam_id, create_time);
