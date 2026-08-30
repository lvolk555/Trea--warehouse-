package com.ailearning.module.ai.controller;

import com.ailearning.common.Result;
import com.ailearning.module.ai.dto.AiChatDTO;
import com.ailearning.module.ai.entity.AiChatMessage;
import com.ailearning.module.ai.entity.AiChatSession;
import com.ailearning.module.ai.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 学生 AI 答疑接口：会话管理 + SSE 流式问答
 */
@RestController
@RequestMapping("/student/ai")
@RequiredArgsConstructor
public class StudentAiController {

    private final AiChatService aiChatService;

    /** 我的会话列表 */
    @GetMapping("/sessions")
    public Result<List<AiChatSession>> sessions() {
        return Result.ok(aiChatService.mySessions());
    }

    /** 会话历史消息 */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<AiChatMessage>> messages(@PathVariable Long sessionId) {
        return Result.ok(aiChatService.sessionMessages(sessionId));
    }

    /** 删除会话 */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        aiChatService.deleteSession(sessionId);
        return Result.ok();
    }

    /**
     * 提问（SSE 流式返回）：首帧 [SESSION:{id}] 下发会话 ID，后续为文本块
     */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ask(@Valid @RequestBody AiChatDTO dto) {
        return aiChatService.ask(dto);
    }
}
