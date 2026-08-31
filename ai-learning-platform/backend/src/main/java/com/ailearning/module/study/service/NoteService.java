package com.ailearning.module.study.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.course.entity.Chapter;
import com.ailearning.module.course.entity.Course;
import com.ailearning.module.course.entity.Video;
import com.ailearning.module.course.mapper.ChapterMapper;
import com.ailearning.module.course.mapper.CourseMapper;
import com.ailearning.module.course.mapper.VideoMapper;
import com.ailearning.module.study.dto.NoteSaveDTO;
import com.ailearning.module.study.entity.StudyNote;
import com.ailearning.module.study.mapper.StudyNoteMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习笔记服务：按视频保存富文本笔记，查询时附带课程/章节/视频标题
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final StudyNoteMapper noteMapper;
    private final VideoMapper videoMapper;
    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;

    /** 保存/更新笔记；内容为空时删除该视频笔记 */
    @Transactional(rollbackFor = Exception.class)
    public String save(NoteSaveDTO dto) {
        long studentId = UserContext.userId();
        Video video = videoMapper.selectById(dto.getVideoId());
        if (video == null) {
            throw new BizException("视频不存在");
        }
        String content = dto.getContent() == null ? "" : dto.getContent().trim();

        StudyNote note = noteMapper.selectOne(new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getStudentId, studentId)
                .eq(StudyNote::getVideoId, dto.getVideoId())
                .last("LIMIT 1"));

        if (content.isEmpty()) {
            if (note != null) {
                noteMapper.deleteById(note.getId());
            }
            return "";
        }

        if (note == null) {
            note = new StudyNote();
            note.setStudentId(studentId);
            note.setVideoId(dto.getVideoId());
            note.setContent(content);
            noteMapper.insert(note);
        } else {
            note.setContent(content);
            noteMapper.updateById(note);
        }
        return content;
    }

    /** 获取某视频的笔记内容（无则返回空字符串） */
    public String getContent(Long videoId) {
        StudyNote note = noteMapper.selectOne(new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getStudentId, UserContext.userId())
                .eq(StudyNote::getVideoId, videoId)
                .last("LIMIT 1"));
        return note != null ? note.getContent() : "";
    }

    /** 删除某条笔记（校验归属） */
    public void delete(Long id) {
        StudyNote note = noteMapper.selectById(id);
        if (note == null || !note.getStudentId().equals(UserContext.userId())) {
            throw new BizException("笔记不存在");
        }
        noteMapper.deleteById(id);
    }

    /** 我的笔记列表：附带课程/章节/视频标题，按时间倒序 */
    public List<Map<String, Object>> list() {
        long studentId = UserContext.userId();
        List<StudyNote> notes = noteMapper.selectList(new LambdaQueryWrapper<StudyNote>()
                .eq(StudyNote::getStudentId, studentId)
                .orderByDesc(StudyNote::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (StudyNote n : notes) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId());
            item.put("videoId", n.getVideoId());
            item.put("content", n.getContent());
            item.put("createTime", n.getCreateTime());

            Video video = videoMapper.selectById(n.getVideoId());
            item.put("videoTitle", video != null ? video.getTitle() : "");
            Long courseId = null;
            String chapterTitle = "";
            String courseTitle = "";
            if (video != null) {
                Chapter chapter = chapterMapper.selectById(video.getChapterId());
                if (chapter != null) {
                    chapterTitle = chapter.getTitle();
                    Course course = courseMapper.selectById(chapter.getCourseId());
                    if (course != null) {
                        courseId = course.getId();
                        courseTitle = course.getTitle();
                    }
                }
            }
            item.put("courseId", courseId);
            item.put("chapterTitle", chapterTitle);
            item.put("courseTitle", courseTitle);
            result.add(item);
        }
        return result;
    }
}