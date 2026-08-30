package com.ailearning.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话实体：学生与 AI 答疑的会话容器，可关联课程上下文
 */
@Data
@TableName("ai_chat_session")
public class AiChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /** 关联课程（可空，为空则为通用答疑） */
    private Long courseId;

    /** 会话标题（取首条提问） */
    private String title;

    private LocalDateTime createTime;
}
