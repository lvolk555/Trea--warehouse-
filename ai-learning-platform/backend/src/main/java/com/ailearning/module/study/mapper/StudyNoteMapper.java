package com.ailearning.module.study.mapper;

import com.ailearning.module.study.entity.StudyNote;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习笔记 Mapper
 */
@Mapper
public interface StudyNoteMapper extends BaseMapper<StudyNote> {
}