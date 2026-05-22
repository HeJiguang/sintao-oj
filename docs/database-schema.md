# 数据库表结构文档

> 本文档描述 BiteOJ 项目中的所有数据库表、字段定义、表间关系以及各表在项目中的接入方式。

---

## 目录

1. [概览](#1-概览)
2. [主应用 Schema: `bitoj_dev`](#2-主应用-schema-bitoj_dev)
   - [2.1 tb_sys_user — 后台管理员用户](#21-tb_sys_user--后台管理员用户)
   - [2.2 tb_user — 前台用户](#22-tb_user--前台用户)
   - [2.3 tb_question — 题目库](#23-tb_question--题目库)
   - [2.4 tb_exam — 考试/竞赛](#24-tb_exam--考试竞赛)
   - [2.5 tb_exam_question — 考试-题目关联](#25-tb_exam_question--考试-题目关联)
   - [2.6 tb_user_exam — 用户-考试关联](#26-tb_user_exam--用户-考试关联)
   - [2.7 tb_user_submit — 用户提交记录](#27-tb_user_submit--用户提交记录)
   - [2.8 tb_notice — 系统公告](#28-tb_notice--系统公告)
   - [2.9 tb_message_text — 消息内容](#29-tb_message_text--消息内容)
   - [2.10 tb_message — 消息投递](#210-tb_message--消息投递)
   - [2.11 tb_training_profile — 训练画像](#211-tb_training_profile--训练画像)
   - [2.12 tb_training_plan — 训练计划](#212-tb_training_plan--训练计划)
   - [2.13 tb_training_task — 训练任务](#213-tb_training_task--训练任务)
   - [2.14 tb_test — 测试表](#214-tb_test--测试表)
3. [题目收录工具 Schema](#3-题目收录工具-schema)
   - [3.1 candidate_problem — 候选题目](#31-candidate_problem--候选题目)
   - [3.2 candidate_judge_case — 候选评测用例](#32-candidate_judge_case--候选评测用例)
   - [3.3 candidate_solution — 候选题解](#33-candidate_solution--候选题解)
   - [3.4 source_artifact — 源素材](#34-source_artifact--源素材)
   - [3.5 review_decision — 审核记录](#35-review_decision--审核记录)
   - [3.6 dedup_match — 去重匹配](#36-dedup_match--去重匹配)
   - [3.7 import_record — 导入记录](#37-import_record--导入记录)
   - [3.8 remote_database_config — 远程数据库配置](#38-remote_database_config--远程数据库配置)
   - [3.9 llm_config — LLM 配置](#39-llm_config--llm-配置)
4. [表关系总图](#4-表关系总图)
5. [项目接入对照表](#5-项目接入对照表)

---

## 1. 概览

项目包含 **两套独立的数据库 Schema**：

| Schema | 用途 | 技术栈 | 位置 |
|---|---|---|---|
| `bitoj_dev` (主应用) | OJ 在线评测系统核心业务 | MySQL + MyBatis-Plus (Java) | `oj-modules/` + `deploy/dev/sql/` |
| tool/question-curation | 题目收录工具 | SQLAlchemy (Python) | `tool/question-curation/` |

主应用共有 **14 张表**（其中 tb_test 为开发测试表），题目收录工具有 **9 张表**。

---

## 2. 主应用 Schema: `bitoj_dev`

### 公共基字段

主应用所有实体继承 `BaseEntity`（`oj-common/oj-common-core/.../domain/BaseEntity.java`），每张表均包含以下审计字段：

| 字段名 | 类型 | 说明 |
|---|---|---|
| `create_by` | `bigint unsigned NOT NULL` | 创建者用户 ID |
| `create_time` | `datetime NOT NULL` | 创建时间 |
| `update_by` | `bigint unsigned DEFAULT NULL` | 更新者用户 ID |
| `update_time` | `datetime DEFAULT NULL` | 更新时间 |

---

### 2.1 tb_sys_user — 后台管理员用户

**定义文件：** `deploy/dev/sql/init-db.sql:30`  
**Java 实体：** `oj-modules/oj-system/src/main/java/com/sintao/system/domain/sysuser/SysUser.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `user_id` | `bigint unsigned PK` | 管理员 ID |
| `user_account` | `varchar(20) UNIQUE` | 登录账号 |
| `nick_name` | `varchar(20)` | 昵称 |
| `password` | `char(60) NOT NULL` | bcrypt 密码哈希 |

**索引：** `idx_user_account` (UNIQUE)

**项目接入：**

| 层 | 文件 | 说明 |
|---|---|---|
| Controller | `oj-modules/oj-system/.../controller/SysUserController.java` | 管理员 CRUD 接口 |
| Service | `oj-modules/oj-system/.../service/impl/SysUserServiceImpl.java` | 登录验证、密码校验 |
| Mapper | `oj-modules/oj-system/.../mapper/SysUserMapper.java` | MyBatis 数据访问 |
| 前端 | `frontend/apps/admin/` | 管理后台，登录页、用户管理页 |

---

### 2.2 tb_user — 前台用户

**定义文件：** `deploy/dev/sql/init-db.sql:105`，经 `2026-03-25-phase1-email-login-user-column-fix.sql` 微调  
**Java 实体：** `oj-modules/oj-system/.../domain/user/User.java`、`oj-modules/oj-friend/.../domain/user/User.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `user_id` | `bigint unsigned PK` | 用户 ID |
| `nick_name` | `varchar(20)` | 昵称 |
| `head_image` | `varchar(200)` | 头像 URL |
| `sex` | `tinyint` | 1=男, 2=女 |
| `phone` | `varchar(20)` | 手机号（可空） |
| `code` | `char(6)` | 邮箱验证码 |
| `email` | `varchar(100)` | 邮箱 |
| `wechat` | `varchar(20)` | 微信号 |
| `school_name` | `varchar(50)` | 学校 |
| `major_name` | `varchar(50)` | 专业 |
| `introduce` | `varchar(255)` | 个人简介 |
| `status` | `tinyint NOT NULL` | 0=封禁, 1=正常 |

**项目接入：**

| 层 | 文件 | 说明 |
|---|---|---|
| Controller | `oj-modules/oj-system/.../controller/UserController.java` | 用户管理（管理后台） |
| Controller | `oj-modules/oj-friend/.../controller/UserController.java` | 用户信息（前台 API） |
| Mapper | `oj-modules/oj-system/.../mapper/UserMapper.java` | 系统模块 Mapper |
| 前端前台 | `frontend/apps/app/` | 用户注册、登录、个人信息 |
| 前端后台 | `frontend/apps/admin/` | 用户列表、状态管理 |

---

### 2.3 tb_question — 题目库

**定义文件：** `deploy/dev/sql/init-db.sql:48`，经 `2026-03-25-phase1-learning-platform-mvp.sql` 增加算法标签等字段  
**Java 实体：** `oj-modules/oj-system/.../domain/question/Question.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `question_id` | `bigint unsigned PK` | 题目 ID |
| `title` | `varchar(50) NOT NULL` | 题目标题 |
| `difficulty` | `tinyint NOT NULL` | 1=简单, 2=中等, 3=困难 |
| `algorithm_tag` | `varchar(100)` | 算法标签 |
| `knowledge_tags` | `varchar(500)` | 知识点标签（逗号分隔） |
| `estimated_minutes` | `int` | 预估解题时间 |
| `training_enabled` | `tinyint NOT NULL DEFAULT 1` | 是否用于训练计划 |
| `time_limit` | `int NOT NULL` | 时间限制（ms） |
| `space_limit` | `int NOT NULL` | 空间限制（KB） |
| `content` | `varchar(1000) NOT NULL` | 题目描述 |
| `question_case` | `varchar(1000)` | 测试用例 JSON |
| `default_code` | `varchar(2000) NOT NULL` | 默认代码模板 |
| `main_fuc` | `varchar(500) NOT NULL` | 入口函数签名 |

**被关联：** `tb_exam_question.question_id`、`tb_user_submit.question_id`、`tb_training_task.question_id`

**项目接入：**

| 层 | 文件 | 说明 |
|---|---|---|
| Controller | `oj-modules/oj-system/.../controller/QuestionController.java` | 题目管理（后台 CRUD） |
| Controller | `oj-modules/oj-friend/.../controller/QuestionController.java` | 题目查询（前台） |
| Mapper | `oj-modules/oj-system/.../mapper/QuestionMapper.java` | 系统模块 Mapper |
| 前端后台 | `frontend/apps/admin/` | 题目编辑器、题目列表 |
| 前端前台 | `frontend/apps/app/` | 题目展示、在线编程 |

---

### 2.4 tb_exam — 考试/竞赛

**定义文件：** `deploy/dev/sql/init-db.sql:74`  
**Java 实体：** `oj-modules/oj-system/.../domain/exam/Exam.java`、`oj-modules/oj-friend/.../domain/exam/Exam.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `exam_id` | `bigint unsigned PK` | 考试 ID |
| `title` | `varchar(50) NOT NULL` | 考试标题 |
| `start_time` | `datetime NOT NULL` | 开始时间 |
| `end_time` | `datetime NOT NULL` | 结束时间 |
| `status` | `tinyint NOT NULL DEFAULT 0` | 0=草稿, 1=已发布 |

**被关联：** `tb_exam_question.exam_id`、`tb_user_exam.exam_id`、`tb_user_submit.exam_id`、`tb_training_profile.last_test_exam_id`、`tb_training_plan.based_on_exam_id`

**项目接入：**

| 层 | 文件 | 说明 |
|---|---|---|
| Controller | `oj-modules/oj-system/.../controller/ExamController.java` | 考试管理（后台 CRUD + 发布） |
| Controller | `oj-modules/oj-friend/.../controller/ExamController.java` | 考试查询、参与（前台） |
| Mapper | `oj-modules/oj-system/.../mapper/ExamMapper.java` | 系统模块 Mapper |
| Mapper | `oj-modules/oj-friend/.../mapper/ExamMapper.java` | 前台模块 Mapper |
| 前端后台 | `frontend/apps/admin/` | 考试编辑器、发布 |
| 前端前台 | `frontend/apps/app/` | 考试列表、考试答题 |

---

### 2.5 tb_exam_question — 考试-题目关联

**定义文件：** `deploy/dev/sql/init-db.sql:88`  
**Java 实体：** `oj-modules/oj-system/.../domain/exam/ExamQuestion.java`、`oj-modules/oj-friend/.../domain/exam/ExamQuestion.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `exam_question_id` | `bigint unsigned PK` | 关联 ID |
| `question_id` | `bigint unsigned NOT NULL` | 题目 ID（关联 tb_question） |
| `exam_id` | `bigint unsigned NOT NULL` | 考试 ID（关联 tb_exam） |
| `question_order` | `int NOT NULL` | 题目排序 |

**逻辑外键：** `exam_id` → `tb_exam.exam_id`，`question_id` → `tb_question.question_id`

---

### 2.6 tb_user_exam — 用户-考试关联

**定义文件：** `deploy/dev/sql/init-db.sql:126`  
**Java 实体：** `oj-modules/oj-job/.../domain/user/UserExam.java`、`oj-modules/oj-friend/.../domain/user/UserExam.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `user_exam_id` | `bigint unsigned PK` | 关联 ID |
| `user_id` | `bigint unsigned NOT NULL` | 用户 ID |
| `exam_id` | `bigint unsigned NOT NULL` | 考试 ID |
| `score` | `int unsigned` | 得分 |
| `exam_rank` | `int unsigned` | 排名 |

**逻辑外键：** `user_id` → `tb_user.user_id`，`exam_id` → `tb_exam.exam_id`

**项目接入：**

| 层 | 文件 | 说明 |
|---|---|---|
| 定时任务 | `oj-modules/oj-job/.../job/ExamRankJob.java` | 考试排名定时计算 |
| Mapper | `oj-modules/oj-job/.../mapper/UserExamMapper.java` | 定时任务 Mapper |
| Mapper | `oj-modules/oj-friend/.../mapper/UserExamMapper.java` | 前台查询 Mapper |

---

### 2.7 tb_user_submit — 用户提交记录（评测记录）

**定义文件：** `deploy/dev/sql/init-db.sql:144`，经以下迁移扩展：
- `2026-03-26-rabbitmq-user-submit-reliability.sql`：增加 `request_id`、`judge_status`、`retry_count`、`last_error`、`finish_time` 及唯一索引
- `2026-03-30-notice-and-judge-history-enhancements.sql`：增加 `use_time`、`use_memory`

**Java 实体：** `oj-modules/oj-judge/.../domain/UserSubmit.java`、`oj-modules/oj-friend/.../domain/user/UserSubmit.java`、`oj-modules/oj-job/.../domain/user/UserSubmit.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `submit_id` | `bigint unsigned PK` | 提交记录 ID |
| `request_id` | `varchar(64) UNIQUE` | 异步评测请求 ID（UUID） |
| `user_id` | `bigint unsigned NOT NULL` | 用户 ID |
| `question_id` | `bigint unsigned NOT NULL` | 题目 ID |
| `exam_id` | `bigint unsigned` | 考试 ID（独立练习时为空） |
| `program_type` | `tinyint NOT NULL` | 0=Java, 1=CPP |
| `user_code` | `text NOT NULL` | 提交的源代码 |
| `pass` | `tinyint NOT NULL` | 0=失败, 1=通过, 2=未交, 3=评测中 |
| `exe_message` | `varchar(500) NOT NULL` | 执行结果消息 |
| `score` | `int NOT NULL DEFAULT 0` | 得分 |
| `case_judge_res` | `text` | 逐用例评测结果 JSON |
| `use_time` | `bigint` | 运行时间（ms） |
| `use_memory` | `bigint` | 内存使用（KB） |
| `judge_status` | `tinyint NOT NULL DEFAULT 0` | 0=等待, 1=成功, 2=死信, 3=发布失败 |
| `retry_count` | `int NOT NULL DEFAULT 0` | 重试次数快照 |
| `last_error` | `varchar(1000)` | 最后错误摘要 |
| `finish_time` | `datetime` | 评测完成时间 |

**索引：** `uk_request_id` (UNIQUE)、`idx_user_question_exam_create` (`user_id`, `question_id`, `exam_id`, `create_time`)

**项目接入：**

| 层 | 文件 | 说明 |
|---|---|---|
| 评测引擎 | `oj-modules/oj-judge/` | 代码评测核心模块 |
| 前台 Mapper | `oj-modules/oj-friend/.../mapper/UserSubmitMapper.java` | 提交记录查询 |
| 定时任务 | `oj-modules/oj-job/.../mapper/UserSubmitMapper.java` | 评测状态轮询 |
| 消息队列 | RabbitMQ 集成，支持可靠投递、死信重试 |

---

### 2.8 tb_notice — 系统公告

**定义文件：** `deploy/dev/sql/2026-03-30-notice-and-judge-history-enhancements.sql:7`  
**Java 实体：** `oj-modules/oj-system/.../domain/notice/Notice.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `notice_id` | `bigint unsigned PK` | 公告 ID |
| `title` | `varchar(100) NOT NULL` | 标题 |
| `content` | `text NOT NULL` | 内容 |
| `category` | `varchar(32) NOT NULL DEFAULT 'Announcement'` | 分类 |
| `is_public` | `tinyint NOT NULL DEFAULT 1` | 0=私密, 1=公开 |
| `is_pinned` | `tinyint NOT NULL DEFAULT 0` | 0=普通, 1=置顶 |
| `status` | `tinyint NOT NULL DEFAULT 0` | 0=草稿, 1=已发布 |
| `publish_time` | `datetime` | 发布时间 |

**索引：** `idx_notice_public_publish`、`idx_notice_pinned_publish`

**项目接入：**

| 层 | 文件 | 说明 |
|---|---|---|
| Controller | `oj-modules/oj-system/.../controller/NoticeController.java` | 公告管理（后台 CRUD） |
| Controller | `oj-modules/oj-friend/.../controller/NoticeController.java` | 公告查询（前台） |
| Mapper | `oj-modules/oj-system/.../mapper/NoticeMapper.java` | 系统模块 Mapper |
| Mapper | `oj-modules/oj-friend/.../mapper/NoticeMapper.java` | 前台模块 Mapper |
| 前端后台 | `frontend/apps/admin/` | 公告编辑器、公告列表 |
| 前端前台 | `frontend/apps/app/` | 公告展示 |

---

### 2.9 tb_message_text — 消息内容

**定义文件：** `deploy/dev/sql/init-db.sql:178`  
**Java 实体：** `oj-modules/oj-friend/.../domain/message/MessageText.java`、`oj-modules/oj-job/.../domain/message/MessageText.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `text_id` | `bigint unsigned PK` | 消息内容 ID |
| `message_title` | `varchar(50) NOT NULL` | 消息标题 |
| `message_content` | `varchar(500) NOT NULL` | 消息正文 |

---

### 2.10 tb_message — 消息投递

**定义文件：** `deploy/dev/sql/init-db.sql:189`  
**Java 实体：** `oj-modules/oj-friend/.../domain/message/Message.java`、`oj-modules/oj-job/.../domain/message/Message.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `message_id` | `bigint unsigned PK` | 消息 ID |
| `text_id` | `bigint unsigned NOT NULL` | 消息内容 ID |
| `send_id` | `bigint unsigned NOT NULL` | 发件人用户 ID |
| `rec_id` | `bigint unsigned NOT NULL` | 收件人用户 ID |

**逻辑外键：** `text_id` → `tb_message_text.text_id`

---

### 2.11 tb_training_profile — 训练画像

**定义文件：** `deploy/dev/sql/2026-03-25-phase1-learning-platform-mvp.sql:21`  
**Java 实体：** `oj-modules/oj-friend/.../domain/training/TrainingProfile.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `profile_id` | `bigint unsigned PK` | 画像 ID |
| `user_id` | `bigint unsigned UNIQUE` | 用户 ID |
| `current_level` | `varchar(32)` | 当前水平 |
| `target_direction` | `varchar(100)` | 目标方向 |
| `weak_points` | `text` | 薄弱点 |
| `strong_points` | `text` | 优势点 |
| `last_test_exam_id` | `bigint unsigned` | 最近测验考试 ID |
| `last_plan_id` | `bigint unsigned` | 最近计划 ID |
| `status` | `tinyint NOT NULL DEFAULT 1` | 0=禁用, 1=启用 |

**索引：** `uk_training_profile_user_id` (UNIQUE)、`idx_training_profile_last_plan_id`、`idx_training_profile_last_test_exam_id`

---

### 2.12 tb_training_plan — 训练计划

**定义文件：** `deploy/dev/sql/2026-03-25-phase1-learning-platform-mvp.sql:42`  
**Java 实体：** `oj-modules/oj-friend/.../domain/training/TrainingPlan.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `plan_id` | `bigint unsigned PK` | 计划 ID |
| `user_id` | `bigint unsigned NOT NULL` | 用户 ID |
| `plan_title` | `varchar(100) NOT NULL` | 计划标题 |
| `plan_goal` | `varchar(255)` | 计划目标 |
| `source_type` | `varchar(32) NOT NULL` | 来源类型（exam/ai/manual） |
| `based_on_exam_id` | `bigint unsigned` | 源考试 ID |
| `plan_status` | `tinyint NOT NULL DEFAULT 0` | 0=待定, 1=进行中, 2=完成, 3=过期 |
| `ai_summary` | `text` | AI 生成总结 |

**索引：** `idx_training_plan_user_id`、`idx_training_plan_status`、`idx_training_plan_exam_id`

---

### 2.13 tb_training_task — 训练任务

**定义文件：** `deploy/dev/sql/2026-03-25-phase1-learning-platform-mvp.sql:62`  
**Java 实体：** `oj-modules/oj-friend/.../domain/training/TrainingTask.java`

| 字段 | 类型 | 说明 |
|---|---|---|
| `task_id` | `bigint unsigned PK` | 任务 ID |
| `plan_id` | `bigint unsigned NOT NULL` | 训练计划 ID |
| `user_id` | `bigint unsigned NOT NULL` | 用户 ID |
| `task_type` | `varchar(20) NOT NULL` | 类型（question/exam/review） |
| `question_id` | `bigint unsigned` | 关联题目 ID |
| `exam_id` | `bigint unsigned` | 关联考试 ID |
| `title_snapshot` | `varchar(100) NOT NULL` | 标题快照 |
| `task_order` | `int NOT NULL` | 执行顺序 |
| `task_status` | `tinyint NOT NULL DEFAULT 0` | 0=待办, 1=完成, 2=跳过 |
| `recommended_reason` | `varchar(500)` | AI 推荐理由 |
| `knowledge_tags_snapshot` | `varchar(500)` | 知识点标签快照 |
| `due_time` | `datetime` | 建议完成时间 |

**索引：** `idx_training_task_plan_id`、`idx_training_task_user_id`、`idx_training_task_status`、`idx_training_task_question_id`、`idx_training_task_exam_id`

---

### 2.14 tb_test — 测试表

**Java 实体：** `oj-modules/oj-system/.../test/domain/TestDomain.java`  
**说明：** 仅在 Java 实体中通过 `@TableName("tb_test")` 声明，无对应 SQL CREATE 语句。属于开发/测试遗留产物，不用于生产。

---

## 3. 题目收录工具 Schema

题目收录工具（`tool/question-curation/`）使用 SQLAlchemy ORM，数据库引擎在运行时决定。所有表围绕 `candidate_problem` 组织，形成一个 **候选题目工作流**。

---

### 3.1 candidate_problem — 候选题目

**定义文件：** `tool/question-curation/app/models/candidate.py:31`  
核心表，记录从外部抓取或导入的候选题目。状态机流程：`DISCOVERED → MATERIAL_COLLECTED → NORMALIZED → DEDUP_CHECKED → ARTIFACTS_GENERATED → REVIEW_READY → APPROVED/REJECTED → IMPORTED`

| 字段 | 类型 | 说明 |
|---|---|---|
| `candidate_id` | `int PK autoincrement` | 主键 |
| `title` | `varchar(255) NOT NULL` | 题目标题 |
| `slug` | `varchar(255) UNIQUE NOT NULL` | URL 标识 |
| `source_type` | `varchar(64) NOT NULL` | 来源类型 |
| `source_platform` | `varchar(64) NOT NULL` | 来源平台 |
| `source_url` | `varchar(1024)` | 源 URL |
| `source_problem_id` | `varchar(255)` | 源题目 ID |
| `status` | `enum` | 收录状态（8 个阶段） |
| `difficulty` | `int` | 难度 |
| `algorithm_tag` | `varchar(255)` | 算法标签 |
| `knowledge_tags` | `text` | 知识点标签 |
| `estimated_minutes` | `int` | 预估解题时间 |
| `time_limit_ms` | `int` | 时间限制（ms） |
| `space_limit_kb` | `int` | 空间限制（KB） |
| `statement_markdown` | `text NOT NULL` | 题目描述 Markdown |
| `question_case_json` | `text` | 测试用例 JSON |
| `default_code_java` | `text` | Java 代码模板 |
| `main_fuc_java` | `text` | Java 入口函数 |
| `solution_outline` | `text` | 解题思路 |
| `solution_code_java` | `text` | Java 题解代码 |
| `upload_status` | `enum` | 上传状态（NOT_READY/READY/UPLOADED/FAILED） |
| `remote_question_id` | `varchar(255)` | 上传后远程题目 ID |
| `upload_error` | `text` | 上传错误信息 |
| `created_at` | `datetime` | 创建时间 |
| `updated_at` | `datetime` | 更新时间 |

---

### 3.2 candidate_judge_case — 候选评测用例

**定义文件：** `tool/question-curation/app/models/judge_case.py:9`  
存储候选题目的测试用例。

| 字段 | 类型 | 说明 |
|---|---|---|
| `case_id` | `int PK autoincrement` | 主键 |
| `candidate_id` | `int FK NOT NULL` | 关联候选题目 |
| `case_type` | `varchar(32) NOT NULL` | 用例类型 |
| `case_order` | `int NOT NULL DEFAULT 0` | 排序 |
| `input_text` | `text NOT NULL` | 输入 |
| `output_text` | `text NOT NULL` | 期望输出 |
| `note` | `text` | 备注 |
| `enabled` | `bool NOT NULL DEFAULT true` | 是否启用 |

---

### 3.3 candidate_solution — 候选题解

**定义文件：** `tool/question-curation/app/models/solution.py:11`

| 字段 | 类型 | 说明 |
|---|---|---|
| `solution_id` | `int PK autoincrement` | 主键 |
| `candidate_id` | `int FK NOT NULL` | 关联候选题目 |
| `language` | `varchar(32) NOT NULL` | 语言 |
| `solution_code` | `text NOT NULL` | 题解代码 |
| `solution_outline` | `text` | 解题思路 |
| `complexity_note` | `text` | 复杂度分析 |
| `correctness_note` | `text` | 正确性说明 |

---

### 3.4 source_artifact — 源素材

**定义文件：** `tool/question-curation/app/models/source_artifact.py:11`

| 字段 | 类型 | 说明 |
|---|---|---|
| `artifact_id` | `int PK autoincrement` | 主键 |
| `candidate_id` | `int FK NOT NULL` | 关联候选题目 |
| `kind` | `varchar(64) NOT NULL` | 素材类型 |
| `source_label` | `varchar(255) NOT NULL` | 来源标签 |
| `url` | `varchar(1024)` | 素材 URL |
| `content_path` | `varchar(1024)` | 内容文件路径 |
| `content_text` | `text` | 内容文本 |
| `metadata_json` | `text` | 元数据 JSON |

---

### 3.5 review_decision — 审核记录

**定义文件：** `tool/question-curation/app/models/review.py:11`

| 字段 | 类型 | 说明 |
|---|---|---|
| `review_id` | `int PK autoincrement` | 主键 |
| `candidate_id` | `int FK NOT NULL` | 关联候选题目 |
| `review_status` | `varchar(32) NOT NULL` | 审核状态 |
| `review_comment` | `text` | 审核意见 |
| `reviewer_name` | `varchar(255)` | 审核人 |
| `reviewed_at` | `datetime NOT NULL` | 审核时间 |

---

### 3.6 dedup_match — 去重匹配

**定义文件：** `tool/question-curation/app/models/dedup_match.py:9`

| 字段 | 类型 | 说明 |
|---|---|---|
| `match_id` | `int PK autoincrement` | 主键 |
| `candidate_id` | `int FK NOT NULL` | 关联候选题目 |
| `matched_question_id` | `int` | 匹配到的题目 ID |
| `matched_title` | `varchar(255) NOT NULL` | 匹配标题 |
| `title_similarity` | `float NOT NULL` | 标题相似度 |
| `semantic_similarity` | `float NOT NULL` | 语义相似度 |
| `tag_similarity` | `float NOT NULL` | 标签相似度 |
| `io_similarity` | `float NOT NULL` | I/O 相似度 |
| `overall_similarity` | `float NOT NULL` | 综合相似度 |
| `decision` | `varchar(64) NOT NULL` | 去重决策 |
| `reason` | `text` | 原因 |

---

### 3.7 import_record — 导入记录

**定义文件：** `tool/question-curation/app/models/import_record.py:11`

| 字段 | 类型 | 说明 |
|---|---|---|
| `import_id` | `int PK autoincrement` | 主键 |
| `candidate_id` | `int FK NOT NULL` | 关联候选题目 |
| `import_status` | `varchar(32) NOT NULL` | 导入状态 |
| `target_question_id` | `int` | 目标题目 ID |
| `remote_config_name` | `varchar(128)` | 远程配置名称 |
| `error_message` | `text` | 错误信息 |
| `sql_snapshot` | `text` | SQL 快照 |
| `imported_at` | `datetime NOT NULL` | 导入时间 |

---

### 3.8 remote_database_config — 远程数据库配置

**定义文件：** `tool/question-curation/app/models/remote_db_config.py:11`  
存储连接到远端 OJ 数据库的配置（用于将候选题目导入到主应用数据库）。

| 字段 | 类型 | 说明 |
|---|---|---|
| `config_id` | `int PK autoincrement` | 主键 |
| `name` | `varchar(128) NOT NULL` | 配置名称 |
| `dialect` | `varchar(32) NOT NULL` | SQLAlchemy dialect |
| `host` | `varchar(255) NOT NULL` | 主机 |
| `port` | `int NOT NULL` | 端口 |
| `database_name` | `varchar(255) NOT NULL` | 数据库名 |
| `username` | `varchar(255) NOT NULL` | 用户名 |
| `password` | `varchar(512) NOT NULL` | 密码 |
| `charset` | `varchar(32) NOT NULL` | 字符集 |
| `is_active` | `bool NOT NULL` | 是否启用 |

---

### 3.9 llm_config — LLM 配置

**定义文件：** `tool/question-curation/app/models/llm_config.py:11`  
存储 LLM API 配置，用于 AI 辅助题目生成和分析。

| 字段 | 类型 | 说明 |
|---|---|---|
| `config_id` | `int PK autoincrement` | 主键 |
| `name` | `varchar(128) NOT NULL` | 配置名称 |
| `enabled` | `bool NOT NULL` | 是否启用 |
| `base_url` | `varchar(512)` | API 地址 |
| `api_key` | `varchar(512)` | API 密钥 |
| `model` | `varchar(255)` | 模型名称 |
| `is_active` | `bool NOT NULL` | 是否激活 |

---

## 4. 表关系总图

### 主应用（bitoj_dev）

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│  tb_sys_user │     │    tb_question   │◄────┤ tb_exam_question│
│  (管理员)     │     │    (题目库)       │     └──────┬───────┘
└──────────────┘     └──┬───────────┬───┘            │
                        │           │                │
                        │           │     ┌──────────┴────────┐
                        │           │     │    tb_exam        │
                        │           │     │  (考试/竞赛)      │
                        │           │     └──────────┬────────┘
                        │           │                │
                        │     ┌─────┴──────┐   ┌─────┴──────┐
                        │     │ tb_user_submit  │ tb_user_exam │
                        │     │ (提交/评测)  │   │ (参与记录)  │
                        │     └─────────────┘   └─────────────┘
                        │
              ┌─────────┴──────────┐
              │  tb_training_task  │
              │  (训练任务)         │
              └─────────┬──────────┘
                        │
              ┌─────────┴──────────┐      ┌──────────────────┐
              │ tb_training_plan   │◄─────│ tb_training_profile │
              │ (训练计划)          │      │ (用户画像)        │
              └────────────────────┘      └──────────────────┘

┌──────────────────┐     ┌──────────────────┐
│ tb_message_text  │◄────┤   tb_message     │
│ (消息内容)        │     │ (消息投递)       │
└──────────────────┘     └──────────────────┘

┌──────────────────┐
│   tb_notice      │
│  (系统公告)       │
└──────────────────┘

tb_user ◄──── 被 tb_user_exam、tb_user_submit、tb_training_profile、
              tb_training_plan、tb_training_task、tb_message 引用
```

### 题目收录工具

```
┌──────────────────────────────────────────────────────────┐
│                    candidate_problem                      │
│                    (候选题目)                              │
└────┬──────────┬──────────┬──────────┬──────────┬─────────┘
     │          │          │          │          │
     ▼          ▼          ▼          ▼          ▼
candidate_  candidate_  source_   review_    dedup_    import_
judge_case  solution   artifact  decision   match     record
(评测用例)   (题解)     (素材)     (审核)     (去重)     (导入)
```

---

## 5. 项目接入对照表

### 主应用模块 → 数据库表

| Java 模块 | 涉及的表 | 说明 |
|---|---|---|
| `oj-system`（系统管理） | tb_sys_user, tb_user, tb_question, tb_exam, tb_exam_question, tb_notice | 后台 CRUD + 管理操作 |
| `oj-friend`（前台 API） | tb_user, tb_question, tb_exam, tb_exam_question, tb_user_exam, tb_user_submit, tb_notice, tb_message, tb_message_text, tb_training_profile, tb_training_plan, tb_training_task | 前台查询、用户交互 |
| `oj-judge`（评测引擎） | tb_user_submit | 代码评测、结果回写 |
| `oj-job`（定时任务） | tb_user_exam, tb_user_submit, tb_message, tb_message_text | 排名计算、状态轮询、消息推送 |

### 前端应用 → API

| 前端应用 | 主要使用的功能 |
|---|---|
| `frontend/apps/admin/`（管理后台） | 管理员登录、题目/考试/公告/用户管理、发布操作 |
| `frontend/apps/app/`（用户前台） | 用户注册登录、题目作答、考试参与、提交记录、训练计划、公告查看 |

### 示例：一次完整的用户答题流程涉及的表

```
tb_user (验证身份)
  → tb_exam (查询考试)
  → tb_exam_question (获取题目列表)
  → tb_question (读取题目内容)
  → tb_user_submit (提交代码，写入提交记录)
  → oj-judge 模块消费 tb_user_submit，执行评测
  → tb_user_submit 更新 (pass, score, use_time 等)
  → tb_user_exam 更新 (score, exam_rank) [定时任务]
```
