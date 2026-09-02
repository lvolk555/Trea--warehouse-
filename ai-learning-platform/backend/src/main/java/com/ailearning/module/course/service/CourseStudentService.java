package com.ailearning.module.course.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.entity.CourseEnrollment;
import com.ailearning.module.course.mapper.CourseEnrollmentMapper;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.user.entity.User;
import com.ailearning.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程学生管理服务（教师 + 管理员）：
 * - 教师可查看/管理自己名下课程的学生；
 * - 管理员可查看/管理全部课程的学生；
 * - 支持查看学生列表、添加学生（按用户名/ID）、调整进度、移除学生。
 */
@Service
@RequiredArgsConstructor
public class CourseStudentService {

    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final UserMapper userMapper;
    private final EnrollmentService enrollmentService;

    /** 权限校验：教师只能操作自己的课程，管理员可操作任意课程 */
    private Course checkCourseAccess(Long courseId) {
        UserContext.checkRole(UserContext.ROLE_TEACHER, UserContext.ROLE_ADMIN);
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException("课程不存在");
        }
        if (UserContext.role() == UserContext.ROLE_TEACHER && course.getTeacherId() != UserContext.userId()) {
            throw new BizException(403, "只能管理自己创建的课程");
        }
        return course;
    }

    /** 课程学生分页（含学生信息与学习进度） */
    public IPage<Map<String, Object>> page(Long courseId, int page, int size, String keyword) {
        checkCourseAccess(courseId);
        IPage<CourseEnrollment> enrollmentPage = enrollmentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getCourseId, courseId)
                        .orderByDesc(CourseEnrollment::getCreateTime));
        IPage<Map<String, Object>> result = new Page<>(enrollmentPage.getCurrent(), enrollmentPage.getSize(),
                enrollmentPage.getTotal());
        List<Long> studentIds = enrollmentPage.getRecords().stream()
                .map(CourseEnrollment::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = studentIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(studentIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> records = enrollmentPage.getRecords().stream()
                .filter(e -> userMap.containsKey(e.getStudentId()))
                .map(e -> {
                    User u = userMap.get(e.getStudentId());
                    Map<String, Object> row = new HashMap<>();
                    row.put("enrollmentId", e.getId());
                    row.put("studentId", u.getId());
                    row.put("username", u.getUsername());
                    row.put("nickname", u.getNickname());
                    row.put("avatar", u.getAvatar());
                    row.put("userStatus", u.getStatus());
                    row.put("progress", e.getProgress());
                    row.put("enrollTime", e.getCreateTime());
                    return row;
                })
                // 关键字过滤（用户名/昵称包含）
                .filter(row -> keyword == null || keyword.isBlank()
                        || String.valueOf(row.get("username")).contains(keyword)
                        || String.valueOf(row.get("nickname")).contains(keyword))
                .collect(Collectors.toList());
        result.setRecords(records);
        return result;
    }

    /** 添加学生到课程（按用户名精确定位或用户 ID，幂等：已在课直接返回） */
    public Map<String, Object> addStudent(Long courseId, AddStudentDTO dto) {
        checkCourseAccess(courseId);
        User student;
        if (dto.getStudentId() != null) {
            student = userMapper.selectById(dto.getStudentId());
            if (student == null) {
                throw new BizException("学生不存在");
            }
        } else if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            student = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, dto.getUsername().trim()).last("LIMIT 1"));
            if (student == null) {
                throw new BizException("用户不存在：" + dto.getUsername());
            }
        } else {
            throw new BizException("请提供学生 ID 或用户名");
        }
        if (student.getRole() != UserContext.ROLE_STUDENT) {
            throw new BizException("仅支持添加学生角色账号");
        }
        CourseEnrollment enrollment = enrollmentService.doEnroll(student.getId(), courseId);
        Map<String, Object> result = new HashMap<>();
        result.put("enrollmentId", enrollment.getId());
        result.put("studentId", student.getId());
        result.put("username", student.getUsername());
        result.put("nickname", student.getNickname());
        result.put("progress", enrollment.getProgress());
        return result;
    }

    /** 可添加的学生候选列表（未选本课的学生，供前端选择） */
    public List<Map<String, Object>> candidates(Long courseId, String keyword) {
        checkCourseAccess(courseId);
        List<Long> enrolledIds = enrollmentMapper.selectList(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getCourseId, courseId))
                .stream().map(CourseEnrollment::getStudentId).collect(Collectors.toList());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserContext.ROLE_STUDENT)
                .eq(User::getStatus, 1)
                .notIn(!enrolledIds.isEmpty(), User::getId, enrolledIds)
                .orderByAsc(User::getUsername)
                .last("LIMIT 50");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        return userMapper.selectList(wrapper).stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", u.getId());
            row.put("username", u.getUsername());
            row.put("nickname", u.getNickname());
            row.put("avatar", u.getAvatar());
            return row;
        }).collect(Collectors.toList());
    }

    /** 更新学生选课信息（当前支持进度调整，0-100） */
    public Map<String, Object> updateStudent(Long courseId, Long enrollmentId, UpdateStudentDTO dto) {
        checkCourseAccess(courseId);
        CourseEnrollment enrollment = getEnrollment(enrollmentId);
        if (!enrollment.getCourseId().equals(courseId)) {
            throw new BizException("该选课记录不属于当前课程");
        }
        if (dto.getProgress() != null) {
            BigDecimal progress = dto.getProgress();
            if (progress.compareTo(BigDecimal.ZERO) < 0 || progress.compareTo(new BigDecimal("100")) > 0) {
                throw new BizException("进度取值 0-100");
            }
            enrollment.setProgress(progress);
        }
        enrollmentMapper.updateById(enrollment);
        Map<String, Object> result = new HashMap<>();
        result.put("enrollmentId", enrollment.getId());
        result.put("progress", enrollment.getProgress());
        return result;
    }

    /** 移除学生（删除选课记录；其学习记录保留但不再计入课程） */
    public void removeStudent(Long courseId, Long enrollmentId) {
        checkCourseAccess(courseId);
        CourseEnrollment enrollment = getEnrollment(enrollmentId);
        if (!enrollment.getCourseId().equals(courseId)) {
            throw new BizException("该选课记录不属于当前课程");
        }
        enrollmentMapper.deleteById(enrollmentId);
    }

    private CourseEnrollment getEnrollment(Long enrollmentId) {
        CourseEnrollment enrollment = enrollmentMapper.selectById(enrollmentId);
        if (enrollment == null) {
            throw new BizException("选课记录不存在");
        }
        return enrollment;
    }

    /** 添加学生请求体 */
    @Data
    public static class AddStudentDTO {
        private Long studentId;
        private String username;
    }

    /** 更新学生请求体 */
    @Data
    public static class UpdateStudentDTO {
        private BigDecimal progress;
    }
}
