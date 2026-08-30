package com.ailearning.module.ai.mapper;

import com.ailearning.module.ai.entity.AiChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 消息 Mapper
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}
