package com.ailearning.module.course.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.dto.CourseSaveDTO;
import com.ailearning.module.course.entity.Chapter;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.entity.Video;
import com.ailearning.module.course.mapper.ChapterMapper;
import com.ailearning.module.course.mapper.CourseEnrollmentMapper;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.course.mapper.VideoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 教师课程服务：创建/编辑课程、维护章节视频结构、提交审核
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final VideoMapper videoMapper;
    private final CourseEnrollmentMapper enrollmentMapper;

    /** 课程状态常量 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_ONLINE = 1;
    public static final int STATUS_OFFLINE = 2;

    /**
     * 查询当前教师的课程列表
     */
    public List<Course> myCourses() {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, UserContext.userId())
                .orderByDesc(Course::getCreateTime));
    }

    /**
     * 创建或更新课程（含章节视频结构），保存后进入待审核状态
     */
    @Transactional(rollbackFor = Exception.class)
    public Course saveCourse(CourseSaveDTO dto) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        // 积分兑换课程必须填写所需积分
        if (dto.getPriceType() == 2 && (dto.getPointsPrice() == null || dto.getPointsPrice() <= 0)) {
            throw new BizException("积分兑换课程需填写所需积分");
        }

        Course course;
        if (dto.getId() == null) {
            course = new Course();
            course.setTeacherId(UserContext.userId());
            course.setStatus(STATUS_PENDING);
        } else {
            course = getOwnCourse(dto.getId());
            // 已上架课程编辑后需重新审核
            course.setStatus(STATUS_PENDING);
        }
        course.setTitle(dto.getTitle());
        course.setCover(dto.getCover());
        course.setCategory(dto.getCategory());
        course.setDescription(dto.getDescription());
        course.setPriceType(dto.getPriceType());
        course.setPointsPrice(dto.getPriceType() == 2 ? dto.getPointsPrice() : 0);

        if (dto.getId() == null) {
            courseMapper.insert(course);
        } else {
            courseMapper.updateById(course);
        }

        saveChapters(course.getId(), dto.getChapters());
        return course;
    }

    /**
     * 保存章节视频结构：按 id 更新、无 id 新增、列表中不存在的删除
     */
    private void saveChapters(Long courseId, List<CourseSaveDTO.ChapterItem> chapters) {
        Set<Long> keepChapterIds = new HashSet<>();
        if (chapters != null) {
            int chapterOrder = 1;
            for (CourseSaveDTO.ChapterItem item : chapters) {
                Chapter chapter = item.getId() != null
                        ? chapterMapper.selectById(item.getId())
                        : new Chapter();
                if (chapter == null || !courseId.equals(chapter.getCourseId())) {
                    chapter = new Chapter();
                }
                chapter.setCourseId(courseId);
                chapter.setTitle(item.getTitle());
                chapter.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : chapterOrder);
                if (chapter.getId() == null) {
                    chapterMapper.insert(chapter);
                } else {
                    chapterMapper.updateById(chapter);
                    keepChapterIds.add(chapter.getId());
                }

                saveVideos(chapter.getId(), item.getVideos());
                chapterOrder++;
            }
        }
        // 删除本次未保留的章节及其视频
        List<Chapter> existing = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, courseId));
        for (Chapter c : existing) {
            if (!keepChapterIds.contains(c.getId())) {
                videoMapper.delete(new LambdaQueryWrapper<Video>().eq(Video::getChapterId, c.getId()));
                chapterMapper.deleteById(c.getId());
            }
        }
    }

    /**
     * 保存章节下的视频：按 id 更新、无 id 新增、列表中不存在的删除
     */
    private void saveVideos(Long chapterId, List<CourseSaveDTO.VideoItem> videos) {
        Set<Long> keepVideoIds = new HashSet<>();
        if (videos != null) {
            int videoOrder = 1;
            for (CourseSaveDTO.VideoItem item : videos) {
                Video video = item.getId() != null
                        ? videoMapper.selectById(item.getId())
                        : new Video();
                if (video == null || !chapterId.equals(video.getChapterId())) {
                    video = new Video();
                }
                video.setChapterId(chapterId);
                video.setTitle(item.getTitle());
                video.setUrl(item.getUrl());
                video.setDuration(item.getDuration() != null ? item.getDuration() : 0);
                video.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : videoOrder);
                if (video.getId() == null) {
                    videoMapper.insert(video);
                } else {
                    videoMapper.updateById(video);
                    keepVideoIds.add(video.getId());
                }
                videoOrder++;
            }
        }
        List<Video> existing = videoMapper.selectList(
                new LambdaQueryWrapper<Video>().eq(Video::getChapterId, chapterId));
        for (Video v : existing) {
            if (!keepVideoIds.contains(v.getId())) {
                videoMapper.deleteById(v.getId());
            }
        }
    }

    /**
     * 提交审核：被驳回/已下架的课程重新提交
     */
    public Course submitReview(Long courseId) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        Course course = getOwnCourse(courseId);
        if (course.getStatus() == STATUS_PENDING) {
            throw new BizException("课程已在审核中");
        }
        course.setStatus(STATUS_PENDING);
        courseMapper.updateById(course);
        return course;
    }

    /**
     * 删除课程：仅待审核/已下架且无人选课时允许
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long courseId) {
        UserContext.checkRole(UserContext.ROLE_TEACHER);
        Course course = getOwnCourse(courseId);
        if (course.getStatus() == STATUS_ONLINE) {
            throw new BizException("已上架课程请先下架再删除");
        }
        Long enrolled = enrollmentMapper.selectCount(
                new LambdaQueryWrapper<com.ailearning.module.course.entity.CourseEnrollment>()
                        .eq(com.ailearning.module.course.entity.CourseEnrollment::getCourseId, courseId));
        if (enrolled > 0) {
            throw new BizException("已有学生选课，无法删除");
        }
        // 删除章节与视频
        List<Chapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, courseId));
        for (Chapter c : chapters) {
            videoMapper.delete(new LambdaQueryWrapper<Video>().eq(Video::getChapterId, c.getId()));
        }
        chapterMapper.delete(new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, courseId));
        courseMapper.deleteById(courseId);
    }

    /**
     * 获取当前教师名下的课程，不存在或非本人则抛异常
     */
    private Course getOwnCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !course.getTeacherId().equals(UserContext.userId())) {
            throw new BizException("课程不存在或无权操作");
        }
        return course;
    }
}
