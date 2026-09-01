package com.ailearning.module.ai.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.ai.dto.AiChatDTO;
import com.ailearning.module.ai.entity.AiChatMessage;
import com.ailearning.module.ai.entity.AiChatSession;
import com.ailearning.module.ai.mapper.AiChatMessageMapper;
import com.ailearning.module.ai.mapper.AiChatSessionMapper;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.points.service.PointsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 答疑服务：会话管理、多轮历史组装、SSE 流式问答、降级策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final CourseMapper courseMapper;
    private final ZhipuAiClient zhipuAiClient;
    private final PointsService pointsService;

    /** 多轮历史最多携带的条数（防止 prompt 过长） */
    private static final int MAX_HISTORY = 10;

    /**
     * 我的会话列表
     */
    public List<AiChatSession> mySessions() {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        return sessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getStudentId, UserContext.userId())
                .orderByDesc(AiChatSession::getCreateTime));
    }

    /**
     * 会话历史消息
     */
    public List<AiChatMessage> sessionMessages(Long sessionId) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        getOwnSession(sessionId);
        return messageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByAsc(AiChatMessage::getCreateTime)
                .orderByAsc(AiChatMessage::getId));
    }

    /**
     * 删除会话（级联删消息）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        getOwnSession(sessionId);
        messageMapper.delete(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }

    /**
     * 流式提问：保存用户消息 → 组装 prompt → SSE 流式输出 → 完成后保存 AI 回答
     * 返回的 Flux 元素为文本块；首帧为 "session:{id}" 便于前端拿到会话 ID
     */
    public Flux<String> ask(AiChatDTO dto) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        long studentId = UserContext.userId();

        // 获取或创建会话
        AiChatSession session = dto.getSessionId() != null
                ? getOwnSession(dto.getSessionId())
                : createSession(studentId, dto.getCourseId(), dto.getQuestion());

        // 保存用户消息
        saveMessage(session.getId(), "user", dto.getQuestion());

        // 组装多轮历史
        List<Map<String, String>> history = buildHistory(session.getId());
        String systemPrompt = buildSystemPrompt(session.getCourseId());

        // 首帧下发会话 ID
        Flux<String> sessionFrame = Flux.just("[SESSION:" + session.getId() + "]");

        if (!zhipuAiClient.isConfigured()) {
            // 降级：未配置 API Key
            String fallback = "AI 服务暂未配置（缺少 ZHIPU_API_KEY），请联系管理员配置后再试。";
            saveMessage(session.getId(), "assistant", fallback);
            return sessionFrame.concatWith(Flux.just(fallback));
        }

        // 流式输出，聚合完整回答用于落库；回答完成后发放 AI 提问积分（每日上限由积分服务控制，防刷）
        StringBuilder fullAnswer = new StringBuilder();
        Flux<String> stream = zhipuAiClient.chatStream(systemPrompt, history)
                .doOnNext(fullAnswer::append)
                .doOnComplete(() -> {
                    saveMessage(session.getId(), "assistant", fullAnswer.toString());
                    pointsService.grantByRule(studentId, "ai_ask", "AI 答疑提问奖励");
                })
                .onErrorResume(e -> {
                    // 降级：大模型超时/异常不阻断，返回友好提示
                    log.error("AI 答疑调用失败", e);
                    String msg = "AI 服务暂时不可用，请稍后重试。";
                    saveMessage(session.getId(), "assistant", msg);
                    return Flux.just(msg);
                });

        return sessionFrame.concatWith(stream);
    }

    /** 创建会话，标题取首条提问的前 30 字 */
    private AiChatSession createSession(long studentId, Long courseId, String firstQuestion) {
        AiChatSession session = new AiChatSession();
        session.setStudentId(studentId);
        session.setCourseId(courseId);
        String title = firstQuestion.length() > 30 ? firstQuestion.substring(0, 30) + "…" : firstQuestion;
        session.setTitle(title);
        sessionMapper.insert(session);
        return session;
    }

    /** 获取本人会话，不存在或非本人抛异常 */
    private AiChatSession getOwnSession(Long sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getStudentId().equals(UserContext.userId())) {
            throw new BizException("会话不存在");
        }
        return session;
    }

    /** 组装多轮历史（最近 N 条，按时间正序） */
    private List<Map<String, String>> buildHistory(Long sessionId) {
        List<AiChatMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByDesc(AiChatMessage::getId)
                .last("LIMIT " + MAX_HISTORY));
        List<Map<String, String>> history = new ArrayList<>();
        // 反转为时间正序
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatMessage m = messages.get(i);
            // 降级提示不入上下文：避免模型把"AI 服务不可用"当作自己说过的话，影响多轮语义
            if ("assistant".equals(m.getRole()) && isFallbackText(m.getContent())) {
                continue;
            }
            Map<String, String> item = new HashMap<>();
            item.put("role", m.getRole());
            item.put("content", m.getContent());
            history.add(item);
        }
        return history;
    }

    /** 降级提示文案前缀（这些消息仅用于前端展示，不进入模型上下文） */
    private boolean isFallbackText(String content) {
        return content == null || content.isBlank()
                || content.startsWith("AI 服务暂时不可用") || content.startsWith("AI 服务暂未配置");
    }

    /** 系统提示词：注入课程上下文 */
    private String buildSystemPrompt(Long courseId) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一名专业的在线学习平台 AI 助教，负责解答学生的课程学习问题。");
        sb.append("回答要求：准确、循序渐进、通俗易懂，涉及代码时给出示例；不要编造不确定的知识。");
        if (courseId != null) {
            Course course = courseMapper.selectById(courseId);
            if (course != null) {
                sb.append("当前学生正在学习课程《").append(course.getTitle()).append("》");
                if (course.getDescription() != null && !course.getDescription().isBlank()) {
                    sb.append("，课程简介：").append(course.getDescription());
                }
                sb.append("。请围绕该课程内容进行答疑。");
            }
        }
        return sb.toString();
    }

    private void saveMessage(Long sessionId, String role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        messageMapper.insert(message);
    }
}
