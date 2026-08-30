package com.ailearning.module.study.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.entity.Chapter;
import com.ailearning.module.course.entity.CourseEnrollment;
import com.ailearning.module.course.entity.Video;
import com.ailearning.module.course.mapper.ChapterMapper;
import com.ailearning.module.course.mapper.CourseEnrollmentMapper;
import com.ailearning.module.course.mapper.VideoMapper;
import com.ailearning.module.study.dto.ProgressReportDTO;
import com.ailearning.module.study.entity.LearningRecord;
import com.ailearning.module.study.mapper.LearningRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 学习进度服务：播放位置上报、断点续播、完课标记、课程完成度重算
 * 说明：完课积分奖励将在阶段五积分模块中通过事件/调用接入，本阶段仅维护学习数据
 */
@Service
@RequiredArgsConstructor
public class StudyService {

    private final LearningRecordMapper learningRecordMapper;
    private final VideoMapper videoMapper;
    private final ChapterMapper chapterMapper;
    private final CourseEnrollmentMapper enrollmentMapper;

    /**
     * 上报播放进度；首次看完时标记完课并重算课程完成度
     */
    @Transactional(rollbackFor = Exception.class)
    public LearningRecord reportProgress(ProgressReportDTO dto) {
        long studentId = UserContext.userId();
        Video video = videoMapper.selectById(dto.getVideoId());
        if (video == null) {
            throw new BizException("视频不存在");
        }

        LearningRecord record = learningRecordMapper.selectOne(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, studentId)
                .eq(LearningRecord::getVideoId, dto.getVideoId())
                .last("LIMIT 1"));
        boolean firstFinish = false;
        if (record == null) {
            record = new LearningRecord();
            record.setStudentId(studentId);
            record.setVideoId(dto.getVideoId());
            record.setPosition(dto.getPosition());
            record.setFinished(0);
            learningRecordMapper.insert(record);
        } else {
            record.setPosition(dto.getPosition());
        }

        // 完课判定：前端显式上报 finished，或播放位置达到视频时长 95%
        boolean reachEnd = video.getDuration() != null && video.getDuration() > 0
                && dto.getPosition() >= video.getDuration() * 0.95;
        if ((Boolean.TRUE.equals(dto.getFinished()) || reachEnd) && record.getFinished() == 0) {
            record.setFinished(1);
            firstFinish = true;
        }
        learningRecordMapper.updateById(record);

        // 首次完课 → 重算所属课程完成度
        if (firstFinish) {
            Chapter chapter = chapterMapper.selectById(video.getChapterId());
            if (chapter != null) {
                refreshCourseProgress(studentId, chapter.getCourseId());
            }
        }
        return record;
    }

    /**
     * 断点续播：返回某视频的播放位置（无记录返回 0）
     */
    public int resumePosition(Long videoId) {
        LearningRecord record = learningRecordMapper.selectOne(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, UserContext.userId())
                .eq(LearningRecord::getVideoId, videoId)
                .last("LIMIT 1"));
        return record != null && record.getFinished() == 0 ? record.getPosition() : 0;
    }

    /**
     * 重算课程完成度：已完成视频数 / 视频总数 × 100
     */
    private void refreshCourseProgress(long studentId, Long courseId) {
        List<Chapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<Chapter>().eq(Chapter::getCourseId, courseId));
        if (chapters.isEmpty()) {
            return;
        }
        List<Long> chapterIds = chapters.stream().map(Chapter::getId).toList();
        List<Video> videos = videoMapper.selectList(
                new LambdaQueryWrapper<Video>().in(Video::getChapterId, chapterIds));
        if (videos.isEmpty()) {
            return;
        }
        List<Long> videoIds = videos.stream().map(Video::getId).toList();
        Long finishedCount = learningRecordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getStudentId, studentId)
                .in(LearningRecord::getVideoId, videoIds)
                .eq(LearningRecord::getFinished, 1));

        BigDecimal progress = BigDecimal.valueOf(finishedCount * 100.0 / videos.size())
                .setScale(2, RoundingMode.HALF_UP);

        CourseEnrollment enrollment = enrollmentMapper.selectOne(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getStudentId, studentId)
                .eq(CourseEnrollment::getCourseId, courseId)
                .last("LIMIT 1"));
        if (enrollment != null) {
            enrollment.setProgress(progress);
            enrollmentMapper.updateById(enrollment);
        }
    }
}
