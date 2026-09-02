<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { UserAddOutlined, SearchOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'
import * as courseApi from '../../api/course'
import * as opsApi from '../../api/ops'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

// ================= 课程选择 =================
const courseLoading = ref(false)
const courses = ref([])
const courseId = ref(null)

async function loadCourses() {
  courseLoading.value = true
  try {
    if (userStore.isAdmin) {
      // 管理员：可管理全部教师创建的课程
      const data = await courseApi.adminCoursePage({ page: 1, size: 200 })
      courses.value = data.records || []
    } else {
      // 教师：仅自己创建的课程
      courses.value = await courseApi.teacherCourseList()
    }
  } catch (e) {
    message.error(e.message)
  } finally {
    courseLoading.value = false
  }
}

const currentCourse = computed(() => courses.value.find(c => c.id === courseId.value))

// ================= 学生列表 =================
const loading = ref(false)
const data = ref({ records: [], total: 0 })
const query = reactive({ page: 1, size: 10, keyword: '' })

const columns = [
  { title: '学生', key: 'student', width: 220 },
  { title: '账号状态', key: 'userStatus', width: 100 },
  { title: '学习进度', key: 'progress', width: 200 },
  { title: '选课时间', dataIndex: 'enrollTime', width: 170 },
  { title: '操作', key: 'action', width: 200 }
]

async function loadStudents() {
  if (!courseId.value) return
  loading.value = true
  try {
    data.value = await opsApi.courseStudents(courseId.value, {
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined
    })
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag) {
  query.page = pag.current
  query.size = pag.pageSize
  loadStudents()
}

function handleSearch() {
  query.page = 1
  loadStudents()
}

watch(courseId, () => {
  query.page = 1
  query.keyword = ''
  if (courseId.value) {
    loadStudents()
  } else {
    data.value = { records: [], total: 0 }
  }
})

// ================= 添加学生 =================
const addVisible = ref(false)
const addSaving = ref(false)
const addMode = ref('select') // select 从候选选择 / username 按用户名添加
const candidateKeyword = ref('')
const candidates = ref([])
const candidateLoading = ref(false)
const selectedStudentId = ref(null)
const inputUsername = ref('')

async function openAdd() {
  addVisible.value = true
  addMode.value = 'select'
  selectedStudentId.value = null
  inputUsername.value = ''
  candidateKeyword.value = ''
  await loadCandidates()
}

async function loadCandidates() {
  candidateLoading.value = true
  try {
    candidates.value = await opsApi.courseStudentCandidates(courseId.value, {
      keyword: candidateKeyword.value || undefined
    })
  } catch (e) {
    message.error(e.message)
  } finally {
    candidateLoading.value = false
  }
}

function handleCandidateSearch(val) {
  candidateKeyword.value = val
  loadCandidates()
}

async function handleAdd() {
  if (addMode.value === 'select' && !selectedStudentId.value) {
    message.warning('请选择要添加的学生')
    return
  }
  if (addMode.value === 'username' && !inputUsername.value.trim()) {
    message.warning('请输入学生用户名')
    return
  }
  addSaving.value = true
  try {
    const payload = addMode.value === 'select'
      ? { studentId: selectedStudentId.value }
      : { username: inputUsername.value.trim() }
    const added = await opsApi.addCourseStudent(courseId.value, payload)
    message.success(`已添加学生：${added.username}`)
    addVisible.value = false
    loadStudents()
  } catch (e) {
    message.error(e.message)
  } finally {
    addSaving.value = false
  }
}

// ================= 调整进度 =================
const progressVisible = ref(false)
const progressSaving = ref(false)
const progressTarget = ref(null)
const progressForm = reactive({ progress: 0 })

function openProgress(record) {
  progressTarget.value = record
  progressForm.progress = Number(record.progress) || 0
  progressVisible.value = true
}

async function handleProgress() {
  progressSaving.value = true
  try {
    await opsApi.updateCourseStudent(courseId.value, progressTarget.value.enrollmentId, {
      progress: progressForm.progress
    })
    message.success('进度已更新')
    progressVisible.value = false
    loadStudents()
  } catch (e) {
    message.error(e.message)
  } finally {
    progressSaving.value = false
  }
}

// ================= 移除学生 =================
function handleRemove(record) {
  Modal.confirm({
    title: '移除学生',
    content: `确定将学生「${record.nickname || record.username}」从本课程移除吗？移除后其学习记录将保留但不再计入课程。`,
    okText: '移除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await opsApi.removeCourseStudent(courseId.value, record.enrollmentId)
        message.success('已移除')
        loadStudents()
      } catch (e) {
        message.error(e.message)
      }
    }
  })
}

onMounted(loadCourses)
</script>

<template>
  <a-card :bordered="false">
    <template #title>
      课程学生管理
      <span v-if="currentCourse" class="course-tag">当前课程：《{{ currentCourse.title }}》</span>
    </template>
    <template #extra>
      <a-button type="primary" :disabled="!courseId" @click="openAdd">
        <UserAddOutlined /> 添加学生
      </a-button>
    </template>

    <!-- 课程选择 + 关键字搜索 -->
    <div class="toolbar">
      <a-select
        v-model:value="courseId"
        :loading="courseLoading"
        show-search
        option-filter-prop="label"
        placeholder="请选择要管理的课程"
        class="course-select"
        :options="courses.map(c => ({ value: c.id, label: c.title }))"
      />
      <a-input-search
        v-model:value="query.keyword"
        placeholder="按用户名 / 昵称搜索学生"
        class="keyword-input"
        :disabled="!courseId"
        allow-clear
        @search="handleSearch"
      />
    </div>

    <a-alert
      v-if="userStore.isAdmin"
      type="info"
      show-icon
      class="tip"
      message="管理员可查看并管理任意教师创建课程的学生；教师仅能管理自己创建的课程。"
    />

    <a-empty v-if="!courseId" description="请先选择课程" style="margin: 60px 0" />

    <a-table
      v-else
      :columns="columns"
      :data-source="data.records"
      :loading="loading"
      row-key="enrollmentId"
      :scroll="{ x: 'max-content' }"
      :pagination="{
        current: query.page,
        pageSize: query.size,
        total: data.total,
        showTotal: t => `共 ${t} 名学生`,
        showSizeChanger: true
      }"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'student'">
          <div class="student-cell">
            <a-avatar :src="record.avatar" size="small">
              {{ (record.nickname || record.username || '?').slice(0, 1) }}
            </a-avatar>
            <div class="student-info">
              <div class="nickname">{{ record.nickname || record.username }}</div>
              <div class="username">{{ record.username }}</div>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'userStatus'">
          <a-tag :color="record.userStatus === 1 ? 'green' : 'red'">
            {{ record.userStatus === 1 ? '正常' : '已禁用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'progress'">
          <a-progress
            :percent="Number(record.progress) || 0"
            :status="(Number(record.progress) || 0) >= 100 ? 'success' : 'active'"
            size="small"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="openProgress(record)"><EditOutlined /> 调整进度</a-button>
            <a-button size="small" danger @click="handleRemove(record)"><DeleteOutlined /> 移除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 添加学生弹窗 -->
    <a-modal
      v-model:open="addVisible"
      title="添加学生到课程"
      :confirm-loading="addSaving"
      ok-text="添加"
      cancel-text="取消"
      @ok="handleAdd"
    >
      <a-radio-group v-model:value="addMode" class="add-mode">
        <a-radio-button value="select">从候选学生选择</a-radio-button>
        <a-radio-button value="username">按用户名添加</a-radio-button>
      </a-radio-group>

      <div v-if="addMode === 'select'" class="add-body">
        <a-input-search
          v-model:value="candidateKeyword"
          placeholder="搜索学生（用户名 / 昵称）"
          allow-clear
          @search="handleCandidateSearch"
        />
        <div class="candidate-list">
          <a-spin :spinning="candidateLoading">
            <div v-if="candidates.length === 0 && !candidateLoading" class="no-candidate">
              暂无可添加的学生
            </div>
            <div
              v-for="c in candidates"
              :key="c.id"
              class="candidate-item"
              :class="{ active: selectedStudentId === c.id }"
              @click="selectedStudentId = c.id"
            >
              <a-avatar :src="c.avatar" size="small">{{ (c.nickname || c.username || '?').slice(0, 1) }}</a-avatar>
              <div class="candidate-info">
                <div>{{ c.nickname || c.username }}</div>
                <div class="candidate-username">{{ c.username }}</div>
              </div>
            </div>
          </a-spin>
        </div>
      </div>

      <div v-else class="add-body">
        <a-input
          v-model:value="inputUsername"
          placeholder="请输入学生用户名（仅支持学生角色账号）"
          allow-clear
          @pressEnter="handleAdd"
        />
        <div class="add-tip">仅支持添加学生角色的账号；若学生已在课程中将自动跳过。</div>
      </div>
    </a-modal>

    <!-- 调整进度弹窗 -->
    <a-modal
      v-model:open="progressVisible"
      :title="`调整进度 - ${progressTarget?.nickname || progressTarget?.username || ''}`"
      :confirm-loading="progressSaving"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleProgress"
    >
      <a-form layout="vertical" style="margin-top: 12px">
        <a-form-item label="学习进度（%）" required>
          <a-input-number
            v-model:value="progressForm.progress"
            :min="0"
            :max="100"
            :precision="0"
            style="width: 100%"
            placeholder="0 - 100"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<style scoped>
.course-tag {
  font-size: 13px;
  font-weight: 400;
  color: #6b7280;
  margin-left: 12px;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.course-select {
  min-width: 280px;
  flex: 1;
}
.keyword-input {
  width: 260px;
}
.tip {
  margin-bottom: 16px;
}
.student-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.student-info .nickname {
  font-weight: 500;
}
.student-info .username {
  font-size: 12px;
  color: #9ca3af;
}
.add-mode {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
.add-body {
  min-height: 120px;
}
.candidate-list {
  margin-top: 12px;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 8px;
}
.candidate-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.candidate-item:hover {
  background: #f5f5f5;
}
.candidate-item.active {
  background: #e6f4ff;
}
.candidate-username {
  font-size: 12px;
  color: #9ca3af;
}
.no-candidate {
  text-align: center;
  color: #9ca3af;
  padding: 24px 0;
}
.add-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #9ca3af;
}
</style>
