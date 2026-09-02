package com.ailearning.module.ops.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.ai.entity.AiChatMessage;
import com.ailearning.module.ai.entity.AiChatSession;
import com.ailearning.module.ai.mapper.AiChatMessageMapper;
import com.ailearning.module.ai.mapper.AiChatSessionMapper;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.entity.CourseEnrollment;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.course.mapper.CourseEnrollmentMapper;
import com.ailearning.module.exam.entity.ExamAnswer;
import com.ailearning.module.exam.entity.ExamRecord;
import com.ailearning.module.exam.entity.PracticeRecord;
import com.ailearning.module.exam.mapper.ExamAnswerMapper;
import com.ailearning.module.exam.mapper.ExamRecordMapper;
import com.ailearning.module.exam.mapper.PracticeRecordMapper;
import com.ailearning.module.ops.entity.CourseComment;
import com.ailearning.module.ops.mapper.CourseCommentMapper;
import com.ailearning.module.points.entity.PointsAccount;
import com.ailearning.module.points.entity.PointsActivityRecord;
import com.ailearning.module.points.entity.PointsRecord;
import com.ailearning.module.points.entity.SignRecord;
import com.ailearning.module.points.entity.UserCoupon;
import com.ailearning.module.points.mapper.PointsAccountMapper;
import com.ailearning.module.points.mapper.PointsActivityRecordMapper;
import com.ailearning.module.points.mapper.PointsRecordMapper;
import com.ailearning.module.points.mapper.SignRecordMapper;
import com.ailearning.module.points.mapper.UserCouponMapper;
import com.ailearning.module.study.entity.LearningRecord;
import com.ailearning.module.study.entity.StudyNote;
import com.ailearning.module.study.mapper.LearningRecordMapper;
import com.ailearning.module.study.mapper.StudyNoteMapper;
import com.ailearning.module.user.entity.User;
import com.ailearning.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 用户管理服务（管理员）：分页查询、新增、编辑、删除、重置密码、启用/禁用、角色调整
 */
@Service
@RequiredArgsConstructor
public class UserManageService {

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final StudyNoteMapper studyNoteMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final PracticeRecordMapper practiceRecordMapper;
    private final PointsAccountMapper pointsAccountMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final SignRecordMapper signRecordMapper;
    private final PointsActivityRecordMapper activityRecordMapper;
    private final UserCouponMapper userCouponMapper;
    private final CourseCommentMapper commentMapper;
    private final AiChatSessionMapper chatSessionMapper;
    private final AiChatMessageMapper chatMessageMapper;

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /** 用户分页（可按角色/状态筛选，关键字匹配用户名或昵称） */
    public IPage<User> page(int page, int size, Integer role, Integer status, String keyword) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(role != null, User::getRole, role)
                .eq(status != null, User::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(User::getUsername, keyword)
                        .or()
                        .like(User::getNickname, keyword))
                .orderByDesc(User::getCreateTime);
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /** 新增用户（管理员可直接指定角色；学生注册走 /auth/register） */
    public User create(UserSaveDTO dto) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        if (dto.getUsername() == null || dto.getUsername().isBlank()
                || dto.getUsername().length() < 3 || dto.getUsername().length() > 50) {
            throw new BizException("用户名需 3-50 位");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new BizException("密码至少 6 位");
        }
        if (dto.getRole() == null || dto.getRole() < 1 || dto.getRole() > 3) {
            throw new BizException("角色不合法");
        }
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? dto.getUsername() : dto.getNickname());
        user.setAvatar(dto.getAvatar());
        user.setRole(dto.getRole());
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    /** 编辑用户（昵称/角色/状态/头像） */
    public User update(Long userId, UserSaveDTO dto) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        User user = getRequired(userId);
        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getRole() != null) {
            if (userId == UserContext.userId()) {
                throw new BizException("不能修改自己的角色");
            }
            if (dto.getRole() < 1 || dto.getRole() > 3) {
                throw new BizException("角色不合法");
            }
            user.setRole(dto.getRole());
        }
        if (dto.getStatus() != null) {
            if (userId == UserContext.userId()) {
                throw new BizException("不能修改自己的账号状态");
            }
            user.setStatus(dto.getStatus());
        }
        userMapper.updateById(user);
        return user;
    }

    /**
     * 删除用户：
     * - 不能删除自己；
     * - 教师名下有课程时拒绝（提示先处理课程）；
     * - 学生删除时同步清理选课/学习记录/考试记录，保证无孤儿数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        if (userId == UserContext.userId()) {
            throw new BizException("不能删除当前登录账号");
        }
        User user = getRequired(userId);
        if (user.getRole() == 2) {
            Long courseCount = courseMapper.selectCount(
                    new LambdaQueryWrapper<Course>().eq(Course::getTeacherId, userId));
            if (courseCount > 0) {
                throw new BizException("该教师名下还有 " + courseCount + " 门课程，请先删除或下架其课程");
            }
        }
        if (user.getRole() == 3) {
            Long adminCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getRole, 3));
            if (adminCount <= 1) {
                throw new BizException("系统至少需要保留一个管理员");
            }
        }
        // 清理该用户在各业务维度的数据，避免孤儿记录
        // 1. AI 会话与消息（先删消息再删会话）
        List<AiChatSession> sessions = chatSessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getStudentId, userId).select(AiChatSession::getId));
        for (AiChatSession session : sessions) {
            chatMessageMapper.delete(new LambdaQueryWrapper<AiChatMessage>()
                    .eq(AiChatMessage::getSessionId, session.getId()));
        }
        chatSessionMapper.delete(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getStudentId, userId));
        // 2. 学习域：选课 / 学习记录 / 笔记
        enrollmentMapper.delete(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getStudentId, userId));
        learningRecordMapper.delete(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, userId));
        studyNoteMapper.delete(new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getStudentId, userId));
        // 3. 考试域：答题（先按 record 关联删）/ 考试记录 / 练习记录
        List<ExamRecord> records = examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, userId).select(ExamRecord::getId));
        for (ExamRecord record : records) {
            examAnswerMapper.delete(new LambdaQueryWrapper<ExamAnswer>()
                    .eq(ExamAnswer::getRecordId, record.getId()));
        }
        examRecordMapper.delete(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getStudentId, userId));
        practiceRecordMapper.delete(new LambdaQueryWrapper<PracticeRecord>()
                .eq(PracticeRecord::getStudentId, userId));
        // 4. 积分域：账户 / 流水 / 签到 / 活动领取 / 优惠券
        pointsAccountMapper.delete(new LambdaQueryWrapper<PointsAccount>()
                .eq(PointsAccount::getUserId, userId));
        pointsRecordMapper.delete(new LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getUserId, userId));
        signRecordMapper.delete(new LambdaQueryWrapper<SignRecord>()
                .eq(SignRecord::getUserId, userId));
        activityRecordMapper.delete(new LambdaQueryWrapper<PointsActivityRecord>()
                .eq(PointsActivityRecord::getUserId, userId));
        userCouponMapper.delete(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId));
        // 5. 评论
        commentMapper.delete(new LambdaQueryWrapper<CourseComment>()
                .eq(CourseComment::getUserId, userId));
        userMapper.deleteById(userId);
    }

    /** 重置密码（管理员设定新密码，无需原密码） */
    public void resetPassword(Long userId, String newPassword) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException("新密码至少 6 位");
        }
        User user = getRequired(userId);
        user.setPassword(PASSWORD_ENCODER.encode(newPassword));
        userMapper.updateById(user);
    }

    /** 启用/禁用用户（不能禁用自己） */
    public User changeStatus(Long userId, boolean enable) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        if (userId == UserContext.userId()) {
            throw new BizException("不能修改自己的账号状态");
        }
        User user = getRequired(userId);
        user.setStatus(enable ? 1 : 0);
        userMapper.updateById(user);
        return user;
    }

    /** 调整角色（1学生 2教师 3管理员，不能修改自己） */
    public User changeRole(Long userId, Integer role) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        if (userId == UserContext.userId()) {
            throw new BizException("不能修改自己的角色");
        }
        if (role == null || role < 1 || role > 3) {
            throw new BizException("角色不合法");
        }
        User user = getRequired(userId);
        user.setRole(role);
        userMapper.updateById(user);
        return user;
    }

    private User getRequired(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    /** 新增/编辑用户请求体 */
    @lombok.Data
    public static class UserSaveDTO {
        private String username;
        private String password;
        private String nickname;
        private String avatar;
        private Integer role;
        private Integer status;
    }
}
