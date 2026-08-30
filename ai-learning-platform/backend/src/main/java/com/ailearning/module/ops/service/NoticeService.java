package com.ailearning.module.ops.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.ops.dto.NoticeSaveDTO;
import com.ailearning.module.ops.entity.Notice;
import com.ailearning.module.ops.mapper.NoticeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公告服务：管理员发布/编辑/撤回/置顶；学生端查看已发布公告
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;

    /** 状态 */
    public static final int STATUS_WITHDRAWN = 0;
    public static final int STATUS_PUBLISHED = 1;

    /** 学生端：已发布公告（置顶优先，时间倒序） */
    public List<Notice> published() {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Notice::getTop)
                .orderByDesc(Notice::getCreateTime));
    }

    /** 管理员：公告分页（可按状态筛选） */
    public IPage<Notice> page(int page, int size, Integer status) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(status != null, Notice::getStatus, status)
                .orderByDesc(Notice::getTop)
                .orderByDesc(Notice::getCreateTime);
        return noticeMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /** 管理员：新建/编辑公告（默认发布） */
    public Notice save(NoticeSaveDTO dto) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        Notice notice = dto.getId() != null ? getRequired(dto.getId()) : new Notice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setType(dto.getType() != null ? dto.getType() : 1);
        notice.setTop(dto.getTop() != null ? dto.getTop() : 0);
        if (notice.getId() == null) {
            notice.setStatus(STATUS_PUBLISHED);
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
        return notice;
    }

    /** 管理员：发布/撤回 */
    public Notice changeStatus(Long id, boolean publish) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        Notice notice = getRequired(id);
        notice.setStatus(publish ? STATUS_PUBLISHED : STATUS_WITHDRAWN);
        noticeMapper.updateById(notice);
        return notice;
    }

    /** 管理员：置顶/取消置顶 */
    public Notice changeTop(Long id, boolean top) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        Notice notice = getRequired(id);
        notice.setTop(top ? 1 : 0);
        noticeMapper.updateById(notice);
        return notice;
    }

    /** 管理员：删除公告 */
    public void delete(Long id) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        getRequired(id);
        noticeMapper.deleteById(id);
    }

    private Notice getRequired(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BizException("公告不存在");
        }
        return notice;
    }
}
