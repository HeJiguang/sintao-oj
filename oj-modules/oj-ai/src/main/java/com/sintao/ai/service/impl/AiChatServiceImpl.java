package com.sintao.ai.service.impl;

import com.sintao.ai.domain.dto.AiChatRequest;
import com.sintao.ai.service.AiChatService;
import com.sintao.ai.tool.AiAssistantTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 组装「题目 + 代码 + 判题结果」上下文，调用 DeepSeek/OpenAI 兼容模型作答。
 * 约定：只给思路与错误分析，不直接给出可通过的完整代码。
 * 支持 Function Calling：可根据关键词推荐题目（search_questions_by_keyword）。
 * 若配置了 RAG（QuestionAnswerAdvisor），会先从向量库检索算法等资料再回答。
 */
@Lazy
@Service("legacyAiChatService")
public class AiChatServiceImpl implements AiChatService {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired(required = false)
    private QuestionAnswerAdvisor questionAnswerAdvisor;

    @Autowired
    private AiAssistantTools aiAssistantTools;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        你是一个在线判题系统（OJ）的做题助手。用户可能提供：当前题目的标题与描述、用户代码、判题结果；也可能只提问，例如推荐题目、算法概念等。

        规则：
        1. 当用户询问“推荐题目”“找某类题”“有没有关于 XXX 的题”时，务必调用工具 `search_questions_by_keyword` 用关键词搜索题目，再根据返回结果推荐并简要说明。
        2. 当用户问算法、数据结构等概念或解题思路时，可结合上下文或检索到的资料（若启用了 RAG）回答，回答简洁清晰，使用中文。
        3. 若有题目描述与用户代码：只根据题目描述、用户代码和判题结果进行分析，不要编造题目或用例；可解释题意、分析错误原因、给出思路或伪代码，但不要直接给出可通过的完整代码。
        4. 若用户未提供判题结果，可针对其代码做逻辑或风格上的简要分析。

        ----- 当前题目与上下文（可能为空） -----
        【题目标题】%s
        【题目描述】%s
        【用户代码】%s
        【判题结果】%s
        ----- 以上为上下文 -----
        """;

    @Override
    public String chat(AiChatRequest request) {
        String systemPrompt = buildSystemPrompt(request);
        ChatClient.Builder builder = chatClientBuilder.defaultSystem(systemPrompt).defaultTools(aiAssistantTools);
        ChatClient client = builder.build();
        var promptSpec = client.prompt().user(request.getUserMessage());
        if (questionAnswerAdvisor != null) {
            promptSpec = promptSpec.advisors(questionAnswerAdvisor);
        }
        return promptSpec.call().content();
    }

    private String buildSystemPrompt(AiChatRequest req) {
        String title = StringUtils.hasText(req.getQuestionTitle()) ? req.getQuestionTitle() : "（未提供）";
        String content = StringUtils.hasText(req.getQuestionContent()) ? req.getQuestionContent() : "（未提供）";
        String code = StringUtils.hasText(req.getUserCode()) ? req.getUserCode() : "（未提供）";
        String judge = StringUtils.hasText(req.getJudgeResult()) ? req.getJudgeResult() : "（无）";
        return String.format(SYSTEM_PROMPT_TEMPLATE, title, content, code, judge);
    }
}

