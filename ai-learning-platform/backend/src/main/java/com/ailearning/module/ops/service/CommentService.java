package com.ailearning.module.ops.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.ops.entity.CourseComment;
import com.ailearning.module.ops.mapper.CourseCommentMapper;
import com.ailearning.module.user.entity.User;
import com.ailearning.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程评论服务：学生发表评论（待审核）、查看已展示评论；管理员审核（展示/隐藏）
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CourseCommentMapper commentMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    /** 状态：0待审核 1已展示 2已隐藏 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_VISIBLE = 1;
    public static final int STATUS_HIDDEN = 2;

    /** 学生发表评论（进入待审核队列） */
    public CourseComment publish(Long courseId, String content) {
        UserContext.checkRole(UserContext.ROLE_STUDENT);
        if (content == null || content.isBlank()) {
            throw new BizException("评论内容不能为空");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException("课程不存在");
        }
        CourseComment comment = new CourseComment();
        comment.setUserId(UserContext.userId());
        comment.setCourseId(courseId);
        comment.setContent(content.trim());
        comment.setStatus(STATUS_PENDING);
        commentMapper.insert(comment);
        return comment;
    }

    /** 课程下已展示评论（带昵称） */
    public List<Map<String, Object>> visibleComments(Long courseId) {
        List<CourseComment> comments = commentMapper.selectList(new LambdaQueryWrapper<CourseComment>()
                .eq(CourseComment::getCourseId, courseId)
                .eq(CourseComment::getStatus, STATUS_VISIBLE)
                .orderByDesc(CourseComment::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (CourseComment c : comments) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("content", c.getContent());
            item.put("createTime", c.getCreateTime());
            User user = userMapper.selectById(c.getUserId());
            item.put("nickname", user != null ? user.getNickname() : "匿名");
            item.put("avatar", user != null ? user.getAvatar() : null);
            result.add(item);
        }
        return result;
    }

    /** 管理员：评论分页（可按状态筛选，带用户与课程信息） */
    public IPage<Map<String, Object>> adminPage(int page, int size, Integer status) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        LambdaQueryWrapper<CourseComment> wrapper = new LambdaQueryWrapper<CourseComment>()
                .eq(status != null, CourseComment::getStatus, status)
                .orderByAsc(CourseComment::getStatus)
                .orderByDesc(CourseComment::getCreateTime);
        IPage<CourseComment> commentPage = commentMapper.selectPage(new Page<>(page, size), wrapper);

        IPage<Map<String, Object>> result = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (CourseComment c : commentPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("content", c.getContent());
            item.put("status", c.getStatus());
            item.put("createTime", c.getCreateTime());
            User user = userMapper.selectById(c.getUserId());
            item.put("nickname", user != null ? user.getNickname() : "");
            Course course = courseMapper.selectById(c.getCourseId());
            item.put("courseTitle", course != null ? course.getTitle() : "");
            records.add(item);
        }
        result.setRecords(records);
        return result;
    }

    /** 管理员：审核评论（展示/隐藏） */
    public CourseComment review(Long id, boolean visible) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        CourseComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BizException("评论不存在");
        }
        comment.setStatus(visible ? STATUS_VISIBLE : STATUS_HIDDEN);
        commentMapper.updateById(comment);
        return comment;
    }

    /** 管理员：删除评论 */
    public void delete(Long id) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        commentMapper.deleteById(id);
    }
}
