<template>
  <div class="profile-overlay" @click.self="$emit('close')">
    <div class="profile-dialog">
      <div class="dialog-header">
        <h3>用户画像</h3>
        <div class="header-actions">
          <el-button type="primary" size="small" :icon="Plus" @click="startAddDimension" v-if="!editingNew && !loading">
            添加维度
          </el-button>
          <el-button text circle :icon="Close" @click="$emit('close')" />
        </div>
      </div>

      <div class="profile-content">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="dimensions.length === 0 && !editingNew" class="empty-state">
          <el-icon :size="48" color="#ccc"><User /></el-icon>
          <p>暂无用户画像数据</p>
          <span class="tip">系统将根据您的对话自动学习并生成画像</span>
        </div>

        <!-- 新增维度表单 -->
        <div v-if="editingNew" class="edit-form">
          <div class="form-group">
            <label>维度名称</label>
            <el-input v-model="newDimensionName" placeholder="如：技术偏好、求职方向" maxlength="30" />
          </div>
          <div class="form-group">
            <label>维度值（逗号分隔多个值）</label>
            <el-input
              v-model="newDimensionValues"
              type="textarea"
              :rows="2"
              placeholder="如：Java, Python, Go"
            />
            <span class="form-hint">使用逗号分隔多个条目值，每个条目独立管理过期时间</span>
          </div>
          <div class="form-actions">
            <el-button @click="cancelAddDimension">取消</el-button>
            <el-button type="primary" :loading="savingNew" @click="saveNewDimension">保存</el-button>
          </div>
        </div>

        <!-- 维度列表 -->
        <div class="profile-list">
          <div
            v-for="dim in dimensions"
            :key="dim.key"
            class="dimension-card"
          >
            <!-- 编辑态 -->
            <div v-if="dim.editing" class="edit-form">
              <div class="form-group">
                <label>维度名称（不可修改）</label>
                <el-input :model-value="dim.key" disabled />
              </div>
              <div class="form-group">
                <label>维度值（逗号分隔多个值）</label>
                <el-input
                  v-model="dim.editValues"
                  type="textarea"
                  :rows="2"
                  placeholder="如：Java, Python, Go"
                />
                <span class="form-hint">使用逗号分隔，已有值保留原过期时间，新增值从当前时间计时</span>
              </div>
              <div class="form-actions">
                <el-button @click="cancelEditDimension(dim)">取消</el-button>
                <el-button type="primary" :loading="dim.saving" @click="saveEditDimension(dim)">保存</el-button>
              </div>
            </div>

            <!-- 展示态 -->
            <template v-else>
              <div class="dimension-header">
                <span class="dimension-key">{{ dim.key }}</span>
                <div class="dimension-actions">
                  <el-button text size="small" :icon="Edit" @click="startEditDimension(dim)" title="编辑维度" />
                  <el-popconfirm
                    title="确定删除该维度？"
                    confirm-button-text="删除"
                    cancel-button-text="取消"
                    @confirm="handleDeleteDimension(dim)"
                  >
                    <template #reference>
                      <el-button text size="small" :icon="Delete" :loading="dim.deleting" title="删除维度" />
                    </template>
                  </el-popconfirm>
                </div>
              </div>

              <div class="dimension-tags">
                <el-tag
                  v-for="entry in dim.entries"
                  :key="entry.value"
                  closable
                  size="large"
                  :disable-transitions="false"
                  @close="handleDeleteEntry(dim, entry)"
                  class="entry-tag"
                >
                  <span class="tag-value">{{ entry.value }}</span>
                  <span class="tag-time">{{ entry.updatedTimeStr }}</span>
                </el-tag>
              </div>
            </template>
          </div>
        </div>
      </div>

      <div class="dialog-footer">
        <el-button type="primary" size="large" @click="$emit('close')">关闭</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Close, Loading, User, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userProfileApi } from '@/api/userProfile'
import { logger } from '@/utils/logger'

const emit = defineEmits(['close'])
const userStore = useUserStore()

const loading = ref(false)
const dimensions = ref([])

// ---- 新增维度 ----
const editingNew = ref(false)
const savingNew = ref(false)
const newDimensionName = ref('')
const newDimensionValues = ref('')

// ---- 数据加载 ----
async function loadUserProfile() {
  if (!userStore.userInfo.userId) {
    ElMessage.warning('请先登录')
    return
  }

  loading.value = true
  try {
    const res = await userProfileApi.getUserProfile()
    if (res.code === 200 && res.data) {
      dimensions.value = groupByKey(res.data.profiles || [])
    } else {
      ElMessage.error(res.message || '获取用户画像失败')
    }
  } catch (error) {
    logger.error('获取用户画像失败:', error)
  } finally {
    loading.value = false
  }
}

function groupByKey(flatItems) {
  const map = {}
  for (const item of flatItems) {
    if (!map[item.key]) {
      map[item.key] = []
    }
    map[item.key].push({
      value: item.value,
      updatedAt: item.updatedAt,
      updatedTimeStr: item.updatedTimeStr
    })
  }
  return Object.entries(map).map(([key, entries]) => ({
    key,
    entries,
    editing: false,
    editValues: '',
    saving: false,
    deleting: false
  }))
}

// ---- 新增维度 ----
function startAddDimension() {
  editingNew.value = true
  newDimensionName.value = ''
  newDimensionValues.value = ''
}

function cancelAddDimension() {
  editingNew.value = false
  newDimensionName.value = ''
  newDimensionValues.value = ''
}

async function saveNewDimension() {
  const name = newDimensionName.value.trim()
  const values = newDimensionValues.value
    .split(/[,，、]/)
    .map(s => s.trim())
    .filter(Boolean)

  if (!name) {
    ElMessage.warning('请输入维度名称')
    return
  }
  if (values.length === 0) {
    ElMessage.warning('请输入至少一个值')
    return
  }

  savingNew.value = true
  try {
    const res = await userProfileApi.updateUserProfile(name, values)
    if (res.code === 200) {
      ElMessage.success('添加成功')
      editingNew.value = false
      newDimensionName.value = ''
      newDimensionValues.value = ''
      await loadUserProfile()
    }
  } catch (error) {
    logger.error('添加维度失败:', error)
  } finally {
    savingNew.value = false
  }
}

// ---- 编辑维度 ----
function startEditDimension(dim) {
  dim.editValues = dim.entries.map(e => e.value).join(', ')
  dim.editing = true
}

function cancelEditDimension(dim) {
  dim.editing = false
  dim.editValues = ''
}

async function saveEditDimension(dim) {
  const values = dim.editValues
    .split(/[,，、]/)
    .map(s => s.trim())
    .filter(Boolean)

  if (values.length === 0) {
    ElMessage.warning('请输入至少一个值，如需删除维度请使用删除按钮')
    return
  }

  dim.saving = true
  try {
    const res = await userProfileApi.updateUserProfile(dim.key, values)
    if (res.code === 200) {
      ElMessage.success('更新成功')
      dim.editing = false
      dim.editValues = ''
      await loadUserProfile()
    }
  } catch (error) {
    logger.error('更新维度失败:', error)
  } finally {
    dim.saving = false
  }
}

// ---- 删除维度 ----
async function handleDeleteDimension(dim) {
  dim.deleting = true
  try {
    const res = await userProfileApi.deleteUserProfile(dim.key)
    if (res.code === 200) {
      ElMessage.success(`已删除维度"${dim.key}"`)
      await loadUserProfile()
    }
  } catch (error) {
    logger.error('删除维度失败:', error)
  } finally {
    dim.deleting = false
  }
}

// ---- 删除单个条目 ----
async function handleDeleteEntry(dim, entry) {
  try {
    const res = await userProfileApi.deleteUserProfile(dim.key, entry.value)
    if (res.code === 200) {
      ElMessage.success(`已删除"${entry.value}"`)
      await loadUserProfile()
    }
  } catch (error) {
    logger.error('删除条目失败:', error)
  }
}

onMounted(() => {
  loadUserProfile()
})
</script>

<style lang="scss" scoped>
.profile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.profile-dialog {
  background-color: #fff;
  border-radius: 16px;
  width: 600px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 24px 16px;
  border-bottom: 1px solid #f0f0f0;

  h3 {
    font-size: 20px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.profile-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  min-height: 200px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
  color: #999;

  .is-loading {
    animation: rotate 1s linear infinite;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
  text-align: center;

  p {
    font-size: 16px;
    color: #666;
    margin: 0;
  }

  .tip {
    font-size: 13px;
    color: #999;
  }
}

.profile-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dimension-card {
  background-color: #f7f8fa;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;

  &:hover {
    background-color: #eef0f2;
  }
}

.dimension-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;

  .dimension-key {
    font-size: 15px;
    font-weight: 600;
    color: #1890ff;
    background-color: rgba(24, 144, 255, 0.1);
    padding: 3px 12px;
    border-radius: 6px;
  }

  .dimension-actions {
    display: flex;
    align-items: center;
    gap: 2px;
  }
}

.dimension-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.entry-tag {
  display: inline-flex !important;
  align-items: center;
  gap: 8px;
  padding: 6px 12px !important;
  height: auto !important;
  font-size: 13px;
  border-radius: 20px;
  cursor: default;

  .tag-value {
    font-weight: 500;
    color: #333;
  }

  .tag-time {
    font-size: 11px;
    color: #999;
    padding-left: 8px;
    border-left: 1px solid #d9d9d9;
  }
}

// ---- 编辑表单 ----
.edit-form {
  background-color: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 4px;
}

.form-group {
  margin-bottom: 12px;

  label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: #666;
    margin-bottom: 5px;
  }

  .form-hint {
    display: block;
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
}

.dialog-footer {
  padding: 16px 24px 24px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
