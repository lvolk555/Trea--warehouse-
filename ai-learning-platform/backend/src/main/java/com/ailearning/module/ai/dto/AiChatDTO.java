package com.ailearning.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 提问请求：会话可空（为空自动创建新会话）
 */
@Data
public class AiChatDTO {

    /** 会话 ID，为空则新建会话 */
    private Long sessionId;

    /** 关联课程（新建会话时使用，可空） */
    private Long courseId;

    @NotBlank(message = "问题不能为空")
    private String question;
}
