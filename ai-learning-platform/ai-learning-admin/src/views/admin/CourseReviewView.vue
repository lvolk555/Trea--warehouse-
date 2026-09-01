<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { CheckOutlined, CloseOutlined, EyeOutlined, PlayCircleOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import * as courseApi from '../../api/course'

const loading = ref(false)
const courses = ref([])
const previewVisible = ref(false)
const previewCourse = ref(null)

// 小节内容查看弹窗
const sectionVisible = ref(false)
const sectionItem = ref(null)

const columns = [
  { title: '课程名称', dataIndex: 'title' },
  { title: '分类', dataIndex: 'category', width: 100 },
  { title: '定价', key: 'price', width: 140 },
  { title: '提交时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 240 }
]

async function loadPending() {
  loading.value = true
  try {
    courses.value = await courseApi.pendingCourses()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

async function handleReview(record, approved) {
  try {
    await courseApi.reviewCourse({ courseId: record.id, approved })
    message.success(approved ? '已通过，课程已上架' : '已驳回')
    loadPending()
  } catch (e) {
    message.error(e.message)
  }
}

async function handlePreview(record) {
  try {
    previewCourse.value = await courseApi.courseDetail(record.id)
    previewVisible.value = true
  } catch (e) {
    message.error(e.message)
  }
}

// 点击小节：弹窗查看内容（视频播放 / 文章渲染）
function openSection(video) {
  sectionItem.value = video
  sectionVisible.value = true
}

onMounted(loadPending)
</script>

<template>
  <a-card title="课程审核" :bordered="false">
    <a-alert message="教师提交/修改课程后进入待审核状态，审核通过后课程上架到学生端课程广场。" type="info" show-icon style="margin-bottom: 16px" />
    <a-table :columns="columns" :data-source="courses" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 'max-content' }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'price'">
          <a-tag v-if="record.priceType === 1" color="blue">免费</a-tag>
          <a-tag v-else color="gold">{{ record.pointsPrice }} 积分</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="handlePreview(record)"><EyeOutlined /> 预览</a-button>
            <a-button size="small" type="primary" @click="handleReview(record, true)"><CheckOutlined /> 通过</a-button>
            <a-button size="small" danger @click="handleReview(record, false)"><CloseOutlined /> 驳回</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 课程预览 -->
    <a-modal v-model:open="previewVisible" :title="`课程预览：${previewCourse?.title || ''}`" width="640px" :footer="null">
      <template v-if="previewCourse">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" bordered>
          <a-descriptions-item label="分类">{{ previewCourse.category }}</a-descriptions-item>
          <a-descriptions-item label="定价">
            {{ previewCourse.priceType === 1 ? '免费' : `${previewCourse.pointsPrice} 积分` }}
          </a-descriptions-item>
          <a-descriptions-item label="课程简介" :span="2">{{ previewCourse.description }}</a-descriptions-item>
        </a-descriptions>
        <a-divider>章节结构（点击小节查看内容）</a-divider>
        <a-collapse>
          <a-collapse-panel v-for="c in previewCourse.chapters" :key="c.id" :header="c.title">
            <div
              v-for="v in c.videos"
              :key="v.id"
              class="video-item clickable"
              @click="openSection(v)"
            >
              <span class="video-name">
                <PlayCircleOutlined v-if="v.sectionType !== 2" class="icon-video" />
                <FileTextOutlined v-else class="icon-article" />
                {{ v.title }}
                <a-tag v-if="v.sectionType === 2" color="purple" class="type-tag">图文</a-tag>
              </span>
              <span v-if="v.sectionType !== 2" class="duration">{{ Math.round((v.duration || 0) / 60) }} 分钟</span>
            </div>
          </a-collapse-panel>
        </a-collapse>
      </template>
    </a-modal>

    <!-- 小节内容查看弹窗 -->
    <a-modal
      v-model:open="sectionVisible"
      :title="sectionItem?.title || '小节内容'"
      width="720px"
      :footer="null"
    >
      <template v-if="sectionItem">
        <!-- 文章小节：渲染文章内容 -->
        <div
          v-if="sectionItem.sectionType === 2"
          class="article-render"
          v-html="sectionItem.articleContent || '<p class=\'empty\'>该文章小节暂无内容</p>'"
        ></div>
        <!-- 视频小节：弹窗内播放 -->
        <template v-else>
          <video
            v-if="sectionItem.url"
            :src="sectionItem.url"
            controls
            autoplay
            class="section-video"
          >
            你的浏览器不支持视频播放
          </video>
          <a-empty v-else description="该视频小节未配置视频地址" />
          <div class="video-meta">时长：约 {{ Math.round((sectionItem.duration || 0) / 60) }} 分钟</div>
        </template>
      </template>
    </a-modal>
  </a-card>
</template>

<style scoped>
.video-item {
  padding: 6px 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 6px;
}
.clickable {
  cursor: pointer;
  transition: background 0.2s;
}
.clickable:hover {
  background: #f5f5f5;
}
.video-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.icon-video {
  color: #1677ff;
}
.icon-article {
  color: #722ed1;
}
.type-tag {
  margin-left: 4px;
}
.duration {
  color: #9ca3af;
  font-size: 12px;
}
.section-video {
  width: 100%;
  border-radius: 8px;
  background: #000;
}
.video-meta {
  margin-top: 8px;
  color: #9ca3af;
  font-size: 12px;
  text-align: center;
}
.article-render {
  line-height: 1.9;
  color: #374151;
  font-size: 14px;
  max-height: 60vh;
  overflow-y: auto;
  word-break: break-word;
}
.article-render :deep(h1) { font-size: 22px; margin: 14px 0 10px; }
.article-render :deep(h2) { font-size: 19px; margin: 12px 0 8px; }
.article-render :deep(h3) { font-size: 16px; margin: 10px 0 6px; }
.article-render :deep(pre) { background: #f6f8fa; padding: 12px; border-radius: 8px; overflow-x: auto; }
.article-render :deep(code) { background: #f6f8fa; padding: 2px 5px; border-radius: 4px; font-family: monospace; }
.article-render :deep(pre code) { background: transparent; padding: 0; }
.article-render :deep(blockquote) { border-left: 3px solid #d0d7de; margin: 10px 0; padding: 4px 14px; color: #57606a; }
.article-render :deep(img) { max-width: 100%; border-radius: 6px; }
.article-render :deep(a) { color: #1677ff; }
.empty {
  color: #9ca3af;
  text-align: center;
  padding: 24px 0;
}
</style>
