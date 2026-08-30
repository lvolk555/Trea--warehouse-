package com.ailearning.module.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 练习/错题记录实体：章节练习即时判分结果，错题进入错题本
 */
@Data
@TableName("practice_record")
public class PracticeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long questionId;

    private String studentAnswer;

    /** 0错 1对 */
    private Integer correct;

    /** 错题是否已标记掌握：0否 1是 */
    private Integer mastered;

    private LocalDateTime createTime;
}
