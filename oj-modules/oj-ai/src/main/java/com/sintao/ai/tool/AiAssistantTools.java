package com.sintao.ai.tool;

import com.sintao.api.RemoteQuestionService;
import com.sintao.common.core.domain.TableDataInfo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AI 助手可调用的工具（Function Calling）。
 * 用于根据关键词检索题目，后续可扩展更多能力。
 */
@Component
public class AiAssistantTools {

    @Autowired
    private RemoteQuestionService remoteQuestionService;

    private static final int DEFAULT_PAGE_SIZE = 10;

    @Tool(
            name = "search_questions_by_keyword",
            description = "根据关键词在题库中搜索题目，返回题目标题、难度等信息。"
    )
    public String searchQuestionsByKeyword(
            @ToolParam(description = "搜索关键词，例如：动态规划、二分查找、数组、链表、递归") String keyword
    ) {
        if (keyword == null || keyword.isBlank()) {
            return "请提供搜索关键词，例如：动态规划、二分查找、数组。";
        }
        try {
            TableDataInfo data = remoteQuestionService.list(
                    keyword.trim(),
                    null,
                    1,
                    DEFAULT_PAGE_SIZE
            );
            if (data == null || data.getRows() == null || data.getRows().isEmpty()) {
                return "未找到与“" + keyword + "”相关的题目，可尝试其它关键词或更通用的词。";
            }
            List<?> rows = data.getRows();
            long total = data.getTotal();
            StringBuilder sb = new StringBuilder();
            sb.append("共找到 ").append(total).append(" 道相关题目（以下展示前 ")
                    .append(rows.size()).append(" 道）：\n");
            for (int i = 0; i < rows.size(); i++) {
                Object row = rows.get(i);
                if (row instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) row;
                    Object id = map.get("questionId");
                    Object title = map.get("title");
                    Object difficulty = map.get("difficulty");
                    String diffStr = difficulty != null ? "难度" + difficulty : "";
                    sb.append(i + 1).append(". [ID:").append(id).append("] ").append(title).append(" ").append(diffStr).append("\n");
                } else {
                    sb.append(i + 1).append(". ").append(row).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询题目失败：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }
}

