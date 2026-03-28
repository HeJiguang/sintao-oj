package com.sintao.ai.domain.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sintao.ai.domain.dto.AiChatRequest;

import java.util.UUID;

public class AgentChatRequest {

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("question_id")
    private String questionId;

    @JsonProperty("question_title")
    private String questionTitle;

    @JsonProperty("question_content")
    private String questionContent;

    @JsonProperty("user_code")
    private String userCode;

    @JsonProperty("judge_result")
    private String judgeResult;

    @JsonProperty("user_message")
    private String userMessage;

    public static AgentChatRequest from(AiChatRequest request) {
        AgentChatRequest agentRequest = new AgentChatRequest();
        agentRequest.setTraceId(UUID.randomUUID().toString());
        agentRequest.setUserId("anonymous");
        agentRequest.setQuestionTitle(request.getQuestionTitle());
        agentRequest.setQuestionContent(request.getQuestionContent());
        agentRequest.setUserCode(request.getUserCode());
        agentRequest.setJudgeResult(request.getJudgeResult());
        agentRequest.setUserMessage(request.getUserMessage());
        return agentRequest;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionTitle() {
        return questionTitle;
    }

    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }

    public String getQuestionContent() {
        return questionContent;
    }

    public void setQuestionContent(String questionContent) {
        this.questionContent = questionContent;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getJudgeResult() {
        return judgeResult;
    }

    public void setJudgeResult(String judgeResult) {
        this.judgeResult = judgeResult;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
}
