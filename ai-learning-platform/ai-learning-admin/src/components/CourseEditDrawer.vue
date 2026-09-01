<script setup>
import { ref, reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, DeleteOutlined, UploadOutlined,
  RobotOutlined, FileMarkdownOutlined, EyeOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import * as courseApi from '../api/course'
import * as aiApi from '../api/ai'
import { uploadFile } from '../api/upload'
import { mdToHtml } from '../utils/markdown'
import RichTextEditor from './RichTextEditor.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  courseId: { type: Number, default: null },
  save: { type: Function, required: true }
})
const emit = defineEmits(['update:open', 'saved'])

const saving = ref(false)
const uploading = reactive({ cover: false, videos: {} })
const form = reactive({
  id: null,
  title: '',
  cover: '',
  category: '编程',
  description: '',
  priceType: 1,
  pointsPrice: 0,
  chapters: []
})

// AI 生成文章弹窗（针对某个文章小节）
const articleModalVisible = ref(false)
const articleGenerating = ref(false)
const articleForm = reactive({ title: '', keywords: '', requirements: '' })
// 当前 AI 生成 / MD 导入 / 预览 目标小节：{ ci, vi }
const targetSection = ref(null)

// 各文章小节的展开预览状态，key 为 `${ci}-${vi}`
const previewMap = reactive({})
const mdInput = ref(null)
let mdTarget = null

function blankForm() {
  return {
    id: null, title: '', cover: '', category: '编程', description: '',
    priceType: 1, pointsPrice: 0,
    chapters: [{ title: '', sortOrder: 1, videos: [] }]
  }
}

function close() {
  emit('update:open', false)
}

async function init() {
  if (!props.open) return
  if (props.courseId) {
    try {
      const d = await courseApi.courseDetail(props.courseId)
      Object.assign(form, {
        id: d.id,
        title: d.title,
        cover: d.cover || '',
        category: d.category || '编程',
        description: d.description || '',
        priceType: d.priceType,
        pointsPrice: d.pointsPrice || 0,
        chapters: (d.chapters || []).map(c => ({
          id: c.id, title: c.title, sortOrder: c.sortOrder,
          videos: (c.videos || []).map(v => ({
            id: v.id,
            title: v.title,
            sectionType: v.sectionType || 1,
            url: v.url || '',
            duration: v.duration || 0,
            articleContent: v.articleContent || '',
            sortOrder: v.sortOrder
          }))
        }))
      })
      if (form.chapters.length === 0) {
        form.chapters = [{ title: '', sortOrder: 1, videos: [] }]
      }
    } catch (e) {
      message.error(e.message)
      close()
    }
  } else {
    Object.assign(form, blankForm())
  }
}

watch(() => props.open, init)
watch(() => props.courseId, init)

// 章节操作
function addChapter() {
  form.chapters.push({ title: '', sortOrder: form.chapters.length + 1, videos: [] })
}
function removeChapter(index) {
  form.chapters.splice(index, 1)
}

// 小节操作：添加视频 / 添加文章
function addVideo(chapter) {
  chapter.videos.push({ title: '', sectionType: 1, url: '', duration: 0, sortOrder: chapter.videos.length + 1 })
}
function addArticle(chapter) {
  chapter.videos.push({ title: '', sectionType: 2, url: '', duration: 0, articleContent: '', sortOrder: chapter.videos.length + 1 })
}
function removeSection(chapter, index) {
  chapter.videos.splice(index, 1)
}

// 上传
async function beforeCoverUpload(file) {
  uploading.cover = true
  try {
    const res = await uploadFile(file)
    form.cover = res.url
    message.success('封面上传成功')
  } catch (e) {
    message.error(e.message)
  } finally {
    uploading.cover = false
  }
  return false
}

async function beforeVideoUpload(ci, vi, file) {
  const key = `${ci}-${vi}`
  uploading.videos[key] = true
  try {
    const res = await uploadFile(file)
    form.chapters[ci].videos[vi].url = res.url
    message.success('视频上传成功')
  } catch (e) {
    message.error(e.message)
  } finally {
    uploading.videos[key] = false
  }
  return false
}

// 导入 MD 文件到指定文章小节
function pickMd(ci, vi) {
  mdTarget = { ci, vi }
  mdInput.value && mdInput.value.click()
}
function onMdFile(e) {
  const file = e.target.files && e.target.files[0]
  if (!file || !mdTarget) return
  const { ci, vi } = mdTarget
  file.text()
    .then(text => {
      form.chapters[ci].videos[vi].articleContent = mdToHtml(text)
      message.success('MD 文件导入成功')
    })
    .catch(() => message.error('读取 MD 文件失败'))
  e.target.value = ''
}

// AI 生成文章：针对指定文章小节
function openArticleModal(ci, vi) {
  targetSection.value = { ci, vi }
  const section = form.chapters[ci].videos[vi]
  articleForm.title = section.title || form.title || ''
  articleForm.keywords = ''
  articleForm.requirements = ''
  articleModalVisible.value = true
}
async function handleGenerateArticle() {
  if (!articleForm.title.trim()) {
    message.warning('请输入文章主题')
    return
  }
  articleGenerating.value = true
  try {
    const md = await aiApi.aiGenerateArticle({ ...articleForm })
    const { ci, vi } = targetSection.value
    const section = form.chapters[ci].videos[vi]
    section.articleContent = mdToHtml(md)
    if (!section.title) section.title = articleForm.title
    if (!form.title) form.title = articleForm.title
    message.success('AI 文章已生成，可继续编辑')
    articleModalVisible.value = false
  } catch (e) {
    message.error(e.message)
  } finally {
    articleGenerating.value = false
  }
}

// 保存
async function handleSave() {
  if (!form.title) {
    message.warning('请填写课程名称')
    return
  }
  if (form.priceType === 2 && (!form.pointsPrice || form.pointsPrice <= 0)) {
    message.warning('积分兑换课程需填写所需积分')
    return
  }
  // 章节与小节标题校验
  for (const chapter of form.chapters) {
    if (!chapter.title.trim()) {
      message.warning('章节标题不能为空')
      return
    }
    for (const v of chapter.videos) {
      if (!v.title.trim()) {
        message.warning('小节标题不能为空（视频与文章小节均需填写标题）')
        return
      }
    }
  }
  saving.value = true
  try {
    await props.save({
      id: form.id,
      title: form.title,
      cover: form.cover,
      category: form.category,
      description: form.description,
      priceType: form.priceType,
      pointsPrice: form.pointsPrice,
      chapters: form.chapters.map(c => ({
        id: c.id, title: c.title, sortOrder: c.sortOrder,
        videos: c.videos.map(v => ({
          id: v.id, title: v.title, sectionType: v.sectionType,
          url: v.sectionType === 2 ? null : v.url,
          duration: v.sectionType === 2 ? 0 : v.duration,
          articleContent: v.sectionType === 2 ? v.articleContent : null,
          sortOrder: v.sortOrder
        }))
      }))
    })
    message.success('保存成功，课程已进入待审核状态')
    close()
    emit('saved')
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <a-drawer
    :open="open"
    :title="form.id ? '编辑课程' : '新建课程'"
    width="760"
    :closable="true"
    @close="close"
  >
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item label="课程名称" required>
            <a-input v-model:value="form.title" placeholder="如：Java 面向对象程序设计" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="分类">
            <a-select v-model:value="form.category" :options="[
              { value: '编程' }, { value: '数学' }, { value: '外语' }, { value: '设计' }, { value: '其他' }
            ]" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="封面图片">
        <a-space direction="vertical" style="width: 100%">
          <div class="cover-upload-row">
            <a-upload :show-upload-list="false" :before-upload="beforeCoverUpload" accept="image/*">
              <a-button :loading="uploading.cover">
                <UploadOutlined /> {{ form.cover ? '重新上传' : '上传封面' }}
              </a-button>
            </a-upload>
            <span class="muted">支持 jpg / png / gif / webp</span>
          </div>
          <a-input v-model:value="form.cover" placeholder="或直接填写图片 URL（https://...）" />
          <img v-if="form.cover" :src="form.cover" class="cover-preview" alt="课程封面预览" />
        </a-space>
      </a-form-item>

      <a-form-item label="课程简介（将作为 AI 答疑的课程上下文）">
        <a-textarea v-model:value="form.description" :rows="3" placeholder="介绍课程内容与目标" />
      </a-form-item>

      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item label="定价方式">
            <a-radio-group v-model:value="form.priceType">
              <a-radio :value="1">免费</a-radio>
              <a-radio :value="2">积分兑换</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" v-if="form.priceType === 2">
          <a-form-item label="所需积分">
            <a-input-number v-model:value="form.pointsPrice" :min="1" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider>章节与小节（视频 / 文章）</a-divider>
      <div v-for="(chapter, ci) in form.chapters" :key="ci" class="chapter-block">
        <div class="chapter-head">
          <a-input v-model:value="chapter.title" :placeholder="`第 ${ci + 1} 章标题`" style="flex: 1" />
          <a-button danger size="small" @click="removeChapter(ci)"><DeleteOutlined /> 删除章节</a-button>
        </div>

        <!-- 视频小节 -->
        <div v-for="(video, vi) in chapter.videos" :key="vi">
          <div v-if="video.sectionType !== 2" class="video-row">
            <a-tag color="blue" class="section-tag">视频</a-tag>
            <a-input v-model:value="video.title" placeholder="视频标题" style="flex: 2" />
            <a-input v-model:value="video.url" placeholder="视频地址 URL" style="flex: 3">
              <template #addonAfter>
                <a-upload :show-upload-list="false" :before-upload="(f) => beforeVideoUpload(ci, vi, f)" accept="video/*">
                  <a-button type="link" size="small" :loading="uploading.videos[`${ci}-${vi}`]" style="padding: 0">
                    <UploadOutlined /> 上传
                  </a-button>
                </a-upload>
              </template>
            </a-input>
            <a-input-number v-model:value="video.duration" :min="0" placeholder="时长(秒)" style="width: 110px" />
            <a-button danger size="small" shape="circle" @click="removeSection(chapter, vi)"><DeleteOutlined /></a-button>
          </div>

          <!-- 文章小节 -->
          <div v-else class="article-row">
            <div class="video-row">
              <a-tag color="purple" class="section-tag"><FileTextOutlined /> 文章</a-tag>
              <a-input v-model:value="video.title" placeholder="文章标题" style="flex: 1" />
              <a-button danger size="small" shape="circle" @click="removeSection(chapter, vi)"><DeleteOutlined /></a-button>
            </div>
            <div class="article-toolbar">
              <a-space wrap>
                <a-button size="small" @click="pickMd(ci, vi)"><FileMarkdownOutlined /> 导入 MD</a-button>
                <a-button size="small" type="primary" ghost @click="openArticleModal(ci, vi)">
                  <RobotOutlined /> AI 生成
                </a-button>
                <a-button size="small" @click="previewMap[`${ci}-${vi}`] = !previewMap[`${ci}-${vi}`]">
                  <EyeOutlined /> {{ previewMap[`${ci}-${vi}`] ? '收起预览' : '预览' }}
                </a-button>
              </a-space>
            </div>
            <RichTextEditor v-model:value="video.articleContent" />
            <div v-if="previewMap[`${ci}-${vi}`]" class="article-preview">
              <a-divider>预览效果</a-divider>
              <div class="article-render" v-html="video.articleContent || '<p class=\'muted\'>暂无内容</p>'"></div>
            </div>
          </div>
        </div>

        <!-- 添加视频 / 添加文章：同款按钮并排 -->
        <a-space style="margin-top: 8px">
          <a-button size="small" @click="addVideo(chapter)"><PlusOutlined /> 添加视频</a-button>
          <a-button size="small" @click="addArticle(chapter)"><PlusOutlined /> 添加文章</a-button>
        </a-space>
      </div>
      <a-button block @click="addChapter"><PlusOutlined /> 添加章节</a-button>
      <input ref="mdInput" type="file" accept=".md,.markdown,text/markdown" style="display: none" @change="onMdFile" />
    </a-form>

    <template #footer>
      <a-space>
        <a-button @click="close">取消</a-button>
        <a-button type="primary" :loading="saving" @click="handleSave">保存（进入待审核）</a-button>
      </a-space>
    </template>

    <!-- AI 生成文章弹窗：输入教程相关信息 -->
    <a-modal v-model:open="articleModalVisible" title="AI 生成教程文章" :width="520" :footer="null">
      <a-form layout="vertical">
        <a-form-item label="文章主题" required>
          <a-input v-model:value="articleForm.title" placeholder="如：C++ 从零到循环" />
        </a-form-item>
        <a-form-item label="关键词 / 知识点（选填）">
          <a-input v-model:value="articleForm.keywords" placeholder="如：变量、循环、函数" />
        </a-form-item>
        <a-form-item label="补充要求（选填）">
          <a-textarea v-model:value="articleForm.requirements" :rows="3" placeholder="如：篇幅、面向对象、含代码示例等" />
        </a-form-item>
      </a-form>
      <div style="text-align: right">
        <a-space>
          <a-button @click="articleModalVisible = false">取消</a-button>
          <a-button type="primary" :loading="articleGenerating" @click="handleGenerateArticle">
            <RobotOutlined /> {{ articleGenerating ? 'AI 生成中…' : '生成' }}
          </a-button>
        </a-space>
      </div>
    </a-modal>
  </a-drawer>
</template>

<style scoped>
.chapter-block {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  background: #fafafa;
}
.chapter-head {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.video-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
.article-row {
  border: 1px dashed #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 10px;
  margin-bottom: 8px;
}
.article-toolbar {
  margin-bottom: 8px;
}
.section-tag {
  flex-shrink: 0;
}
.cover-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.muted {
  color: #9ca3af;
  font-size: 12px;
}
.cover-preview {
  width: 160px;
  height: 90px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
.article-preview .article-render {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
  line-height: 1.8;
}
.article-render :deep(h1) { font-size: 22px; margin: 8px 0; }
.article-render :deep(h2) { font-size: 18px; margin: 8px 0; }
.article-render :deep(h3) { font-size: 16px; margin: 6px 0; }
.article-render :deep(pre) { background: #f6f8fa; padding: 10px; border-radius: 6px; overflow-x: auto; }
.article-render :deep(code) { background: #f6f8fa; padding: 1px 4px; border-radius: 4px; }
.article-render :deep(blockquote) { border-left: 3px solid #d0d7de; margin: 8px 0; padding: 4px 12px; color: #57606a; }
.article-render :deep(img) { max-width: 100%; }

@media (max-width: 768px) {
  .chapter-head,
  .video-row,
  .cover-upload-row {
    flex-wrap: wrap;
  }
  .chapter-head > *,
  .video-row > * {
    flex: 1 1 100% !important;
    width: 100% !important;
  }
}
</style>
