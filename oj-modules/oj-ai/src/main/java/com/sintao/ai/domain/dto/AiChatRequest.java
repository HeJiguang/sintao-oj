package com.sintao.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 做题助手请求体：题目上下文 + 用户代码 + 判题结果（可选）+ 用户问题。
 */
@Data
public class AiChatRequest {

    /** 题目标题（用于上下文，可为空） */
    private String questionTitle;

    /** 题目描述/内容（可为空） */
    private String questionContent;

    /** 用户当前代码（可为空） */
    private String userCode;

    /** 最近一次判题结果（可选，例如 WA 的用例/错误信息等） */
    private String judgeResult;

    /** 用户输入的问题（必填） */
    @NotBlank(message = "用户问题不能为空")
    private String userMessage;
}

