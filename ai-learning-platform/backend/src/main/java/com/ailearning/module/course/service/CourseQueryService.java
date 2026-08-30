package com.ailearning.module.course.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.dto.CourseVO;
import com.ailearning.module.course.entity.Chapter;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.entity.CourseEnrollment;
import com.ailearning.module.course.entity.Video;
import com.ailearning.module.course.mapper.ChapterMapper;
import com.ailearning.module.course.mapper.CourseEnrollmentMapper;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.course.mapper.VideoMapper;
import com.ailearning.module.study.entity.LearningRecord;
import com.ailearning.module.study.mapper.LearningRecordMapper;
import com.ailearning.module.user.entity.User;
import com.ailearning.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程查询服务：学生端课程广场、课程详情、我的课程
 */
@Service
@RequiredArgsConstructor
public class CourseQueryService {

    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final VideoMapper videoMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final UserMapper userMapper;

    /**
     * 课程广场：分页查询已上架课程，支持关键字与分类筛选
     */
    public IPage<CourseVO> courseSquare(int page, int size, String keyword, String category) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, CourseService.STATUS_ONLINE)
                .like(StringUtils.hasText(keyword), Course::getTitle, keyword)
                .eq(StringUtils.hasText(category), Course::getCategory, category)
                .orderByDesc(Course::getCreateTime);
        IPage<Course> coursePage = courseMapper.selectPage(new Page<>(page, size), wrapper);
        return coursePage.convert(this::toVO);
    }

    /**
     * 课程详情：课程信息 + 章节视频树 + 当前学生学习进度
     */
    public CourseVO courseDetail(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException("课程不存在");
        }
        // 未上架课程仅授课教师本人可见（用于预览）
        if (!course.getStatus().equals(CourseService.STATUS_ONLINE)) {
            boolean isOwner = UserContext.role() == UserContext.ROLE_TEACHER
                    && course.getTeacherId().equals(UserContext.userId());
            boolean isAdmin = UserContext.role() == UserContext.ROLE_ADMIN;
            if (!isOwner && !isAdmin) {
                throw new BizException("课程未上架");
            }
        }

        CourseVO vo = toVO(course);

        // 章节视频树
        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getCourseId, courseId).orderByAsc(Chapter::getSortOrder));
        if (!chapters.isEmpty()) {
            List<Long> chapterIds = chapters.stream().map(Chapter::getId).toList();
            List<Video> videos = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                    .in(Video::getChapterId, chapterIds).orderByAsc(Video::getSortOrder));
            Map<Long, List<Video>> videoMap = videos.stream()
                    .collect(Collectors.groupingBy(Video::getChapterId));

            // 当前学生的完成状态
            Map<Long, LearningRecord> recordMap = loadLearningRecords(courseId);

            List<CourseVO.ChapterVO> chapterVOs = new ArrayList<>();
            for (Chapter chapter : chapters) {
                CourseVO.ChapterVO chapterVO = new CourseVO.ChapterVO();
                BeanUtils.copyProperties(chapter, chapterVO);
                List<CourseVO.VideoVO> videoVOs = new ArrayList<>();
                for (Video video : videoMap.getOrDefault(chapter.getId(), List.of())) {
                    CourseVO.VideoVO videoVO = new CourseVO.VideoVO();
                    BeanUtils.copyProperties(video, videoVO);
                    LearningRecord record = recordMap.get(video.getId());
                    videoVO.setFinished(record != null && record.getFinished() == 1);
                    videoVOs.add(videoVO);
                }
                chapterVO.setVideos(videoVOs);
                chapterVOs.add(chapterVO);
            }
            vo.setChapters(chapterVOs);
        }
        return vo;
    }

    /**
     * 我的课程：当前学生已选课程列表（含完成度）
     */
    public List<CourseVO> myCourses() {
        long studentId = UserContext.userId();
        List<CourseEnrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getStudentId, studentId)
                        .orderByDesc(CourseEnrollment::getCreateTime));
        List<CourseVO> result = new ArrayList<>();
        for (CourseEnrollment e : enrollments) {
            Course course = courseMapper.selectById(e.getCourseId());
            if (course == null) {
                continue;
            }
            CourseVO vo = toVO(course);
            vo.setEnrolled(true);
            vo.setProgress(e.getProgress());
            result.add(vo);
        }
        return result;
    }

    /**
     * 课程转 VO：补充教师昵称、视频数、选课状态
     */
    private CourseVO toVO(Course course) {
        CourseVO vo = new CourseVO();
        BeanUtils.copyProperties(course, vo);
        User teacher = userMapper.selectById(course.getTeacherId());
        vo.setTeacherName(teacher != null ? teacher.getNickname() : null);

        List<Chapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, course.getId()));
        if (!chapters.isEmpty()) {
            List<Long> chapterIds = chapters.stream().map(Chapter::getId).toList();
            Long videoCount = videoMapper.selectCount(new LambdaQueryWrapper<Video>()
                    .in(Video::getChapterId, chapterIds));
            vo.setVideoCount(videoCount.intValue());
        } else {
            vo.setVideoCount(0);
        }

        // 当前学生选课状态
        CourseEnrollment enrollment = enrollmentMapper.selectOne(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getStudentId, UserContext.userId())
                .eq(CourseEnrollment::getCourseId, course.getId())
                .last("LIMIT 1"));
        vo.setEnrolled(enrollment != null);
        vo.setProgress(enrollment != null ? enrollment.getProgress() : null);
        return vo;
    }

    /**
     * 加载当前学生在指定课程下的学习记录（视频 ID → 记录）
     */
    private Map<Long, LearningRecord> loadLearningRecords(Long courseId) {
        List<Chapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, courseId));
        if (chapters.isEmpty()) {
            return Map.of();
        }
        List<Long> chapterIds = chapters.stream().map(Chapter::getId).toList();
        List<Video> videos = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .in(Video::getChapterId, chapterIds));
        if (videos.isEmpty()) {
            return Map.of();
        }
        List<Long> videoIds = videos.stream().map(Video::getId).toList();
        List<LearningRecord> records = learningRecordMapper.selectList(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, UserContext.userId())
                .in(LearningRecord::getVideoId, videoIds));
        return records.stream().collect(Collectors.toMap(LearningRecord::getVideoId, r -> r));
    }
}
