<template>
  <div class="user-info-overlay" @click.self="$emit('close')">
    <div class="user-info-dialog">
      <div class="dialog-header">
        <h3>用户基本信息</h3>
        <el-button text circle :icon="Close" @click="$emit('close')" />
      </div>

      <div class="user-info-content">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <!-- 用户信息表单 -->
        <div v-else class="user-info-form">
          <!-- 头像区域 -->
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <el-avatar 
                :size="80" 
                :src="formData.avatar || ''"
                class="user-avatar"
              >
                <el-icon :size="40"><UserFilled /></el-icon>
              </el-avatar>
              <div class="avatar-upload" @click="triggerAvatarUpload">
                <el-icon><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </div>
            <input
              ref="avatarInputRef"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleAvatarChange"
            />
          </div>

          <!-- 用户名字段 -->
          <div class="form-item-with-action">
            <div class="form-item-content">
              <label class="form-label">用户名</label>
              <el-input
                v-model="formData.username"
                placeholder="请输入用户名"
                maxlength="20"
                class="form-input"
              />
            </div>
            <el-button 
              type="primary" 
              :loading="savingField === 'username'"
              @click="saveSingleField('username')"
            >
              修改
            </el-button>
          </div>

          <!-- 昵称字段 -->
          <div class="form-item-with-action">
            <div class="form-item-content">
              <label class="form-label">
                昵称
                <span class="nickname-display">（显示名称：{{ formData.nickName || '未设置' }}）</span>
              </label>
              <el-input
                v-model="formData.nickName"
                placeholder="请输入昵称"
                maxlength="20"
                show-word-limit
                class="form-input"
              />
            </div>
            <el-button 
              type="primary" 
              :loading="savingField === 'nickName'"
              @click="saveSingleField('nickName')"
            >
              修改
            </el-button>
          </div>

          <!-- 手机号字段 -->
          <div class="form-item-with-action">
            <div class="form-item-content">
              <label class="form-label">手机号</label>
              <el-input
                v-model="formData.phone"
                placeholder="请输入手机号"
                maxlength="11"
                class="form-input"
              />
            </div>
            <el-button 
              type="primary" 
              :loading="savingField === 'phone'"
              @click="saveSingleField('phone')"
            >
              修改
            </el-button>
          </div>

          <!-- AI模型参数字段 -->
          <div class="form-section">
            <div class="section-title">AI 模型参数</div>

            <div class="form-item">
              <label class="form-label">Temperature (随机性)</label>
              <el-slider
                v-model="formData.temperature"
                :min="0"
                :max="2"
                :step="0.1"
                show-input
                class="form-slider"
              />
              <span class="form-tip">值越高，回答越随机创造性；值越低，回答越确定保守</span>
            </div>

            <div class="form-item">
              <label class="form-label">Top K</label>
              <el-slider
                v-model="formData.top_k"
                :min="1"
                :max="100"
                :step="1"
                show-input
                class="form-slider"
              />
            </div>

            <div class="form-item">
              <label class="form-label">Top P (核采样)</label>
              <el-slider
                v-model="formData.top_p"
                :min="0"
                :max="1"
                :step="0.01"
                show-input
                class="form-slider"
              />
            </div>

            <!-- AI参数保存按钮 -->
            <div class="ai-params-actions">
              <el-button 
                type="primary" 
                size="large"
                :loading="savingAIParams"
                @click="saveAIParams"
              >
                保存AI参数
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="dialog-footer">
        <el-button size="large" @click="$emit('close')">关闭</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Close, Loading, UserFilled, Camera } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import { modelApi } from '@/api/model'

const emit = defineEmits(['close', 'user-updated'])
const userStore = useUserStore()

const loading = ref(false)
const savingField = ref('') // 当前正在保存的字段
const savingAIParams = ref(false) // 是否正在保存AI参数
const avatarInputRef = ref(null)
const uploadingAvatar = ref(false)

const loadingModelConfig = ref(false)

const formData = reactive({
  username: '',
  nickName: '',
  avatar: '',
  phone: '',
  temperature: 0.7,
  top_k: 50,
  top_p: 0.9,
  max_tokens: 200000,
  thinking_budget: 0,
  enable_thinking: false,
  enable_search: false
})

// 初始化表单数据
function initFormData() {
  const userInfo = userStore.userInfo
  formData.username = userInfo.username || ''
  formData.nickName = userInfo.nickName || ''
  formData.avatar = userInfo.avatar || ''
  formData.phone = userInfo.phone || ''
}

// 加载用户模型配置
async function loadModelConfig() {
  loadingModelConfig.value = true
  try {
    const res = await modelApi.getModelConfig()
    if (res.code === 200 && res.data) {
      formData.temperature = res.data.temperature !== undefined ? Number(res.data.temperature) : 0.7
      formData.top_k = res.data.topK !== undefined ? Number(res.data.topK) : 50
      formData.top_p = res.data.topP !== undefined ? Number(res.data.topP) : 0.9
      formData.max_tokens = res.data.maxTokens !== undefined ? Number(res.data.maxTokens) : 200000
      formData.thinking_budget = res.data.thinkingBudget !== undefined ? Number(res.data.thinkingBudget) : 0
      formData.enable_thinking = res.data.enableThinking !== undefined ? Boolean(res.data.enableThinking) : false
      formData.enable_search = res.data.enableSearch !== undefined ? Boolean(res.data.enableSearch) : false
    }
  } catch (error) {
    console.error('获取模型配置失败:', error)
  } finally {
    loadingModelConfig.value = false
  }
}

// 加载用户信息
async function loadUserInfo() {
  if (!userStore.userInfo.userId) {
    ElMessage.warning('请先登录')
    return
  }

  loading.value = true
  try {
    const res = await userApi.getUserInfo(userStore.userInfo.userId)
    if (res.code === 200 && res.data) {
      // 更新 store 中的用户信息
      userStore.setUserInfo({
        ...userStore.userInfo,
        ...res.data
      })
      initFormData()
    } else {
      ElMessage.error(res.message || '获取用户信息失败')
      // 使用 store 中的数据
      initFormData()
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败，使用本地缓存数据')
    initFormData()
  } finally {
    loading.value = false
  }
}

// 触发头像上传
function triggerAvatarUpload() {
  avatarInputRef.value?.click()
}

// 处理头像变更
async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }

  // 验证文件大小（最大 5MB）
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }

  uploadingAvatar.value = true
  try {
    // 1. 先上传头像文件
    const uploadRes = await userApi.fileUpload(file)
    if (uploadRes.code === 200 && uploadRes.data) {
      const avatarUrl = uploadRes.data
      
      // 2. 更新用户头像信息到后端
      const userVo = {
        username: formData.username,
        nickName: formData.nickName,
        avatar: avatarUrl,
        phone: formData.phone
      }
      
      const updateRes = await userApi.updateUserInfo(userVo)
      if (updateRes.code === 200) {
        // 3. 更新本地表单和store
        formData.avatar = avatarUrl
        userStore.setUserInfo({
          ...userStore.userInfo,
          avatar: avatarUrl
        })
        ElMessage.success('头像更新成功')
        emit('user-updated', { avatar: avatarUrl })
      } else {
        ElMessage.error(updateRes.message || '头像信息保存失败')
      }
    } else {
      ElMessage.error(uploadRes.message || '头像上传失败')
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败，请重试')
  } finally {
    uploadingAvatar.value = false
    // 清空 input 值，允许重复选择同一文件
    event.target.value = ''
  }
}

// 保存单个字段 - 只发送该字段
async function saveSingleField(fieldName) {
  const value = formData[fieldName]

  // 字段验证
  if (fieldName === 'username') {
    if (!value || value.trim().length < 3) {
      ElMessage.warning('用户名至少需要3个字符')
      return
    }
    if (value.trim().length > 20) {
      ElMessage.warning('用户名不能超过20个字符')
      return
    }
  }

  if (fieldName === 'phone') {
    if (value && !/^1[3-9]\d{9}$/.test(value)) {
      ElMessage.warning('请输入正确的手机号')
      return
    }
  }

  savingField.value = fieldName
  try {
    // 只发送要更新的字段，其他字段为null
    const userVo = {
      username: fieldName === 'username' ? value : null,
      nickName: fieldName === 'nickName' ? value : null,
      avatar: null,
      phone: fieldName === 'phone' ? value : null
    }

    const res = await userApi.updateUserInfo(userVo)
    if (res.code === 200) {
      // 更新 store 中的用户信息
      userStore.setUserInfo({
        ...userStore.userInfo,
        [fieldName]: value
      })
      ElMessage.success(`${getFieldLabel(fieldName)}更新成功`)
      emit('user-updated', { [fieldName]: value })
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (error) {
    console.error(`更新${fieldName}失败:`, error)
    ElMessage.error('更新失败，请重试')
  } finally {
    savingField.value = ''
  }
}

// 保存AI参数 - 调用模型配置接口
async function saveAIParams() {
  savingAIParams.value = true
  try {
    const config = {
      temperature: formData.temperature,
      topK: formData.top_k,
      topP: formData.top_p,
      maxTokens: formData.max_tokens,
      thinkingBudget: formData.thinking_budget,
      enableThinking: formData.enable_thinking,
      enableSearch: formData.enable_search
    }

    const res = await modelApi.saveModelConfig(config)
    if (res.code === 200) {
      ElMessage.success('AI模型参数更新成功')
      emit('user-updated', {
        temperature: formData.temperature,
        top_k: formData.top_k,
        top_p: formData.top_p
      })
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (error) {
    console.error('更新AI参数失败:', error)
    ElMessage.error('更新失败，请重试')
  } finally {
    savingAIParams.value = false
  }
}

// 获取字段中文名
function getFieldLabel(fieldName) {
  const labels = {
    username: '用户名',
    nickName: '昵称',
    phone: '手机号'
  }
  return labels[fieldName] || fieldName
}

onMounted(() => {
  loadUserInfo()
  loadModelConfig()
})
</script>

<style lang="scss" scoped>
.user-info-overlay {
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

.user-info-dialog {
  background-color: #fff;
  border-radius: 16px;
  width: 560px;
  max-width: 90vw;
  max-height: 85vh;
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
}

.user-info-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
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

.user-info-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 8px;

  .avatar-wrapper {
    position: relative;
    cursor: pointer;

    .user-avatar {
      border: 3px solid #e5e7eb;
      transition: all 0.3s ease;
    }

    .avatar-upload {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      background-color: rgba(0, 0, 0, 0.6);
      color: #fff;
      padding: 6px 0;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      font-size: 12px;
      border-bottom-left-radius: 40px;
      border-bottom-right-radius: 40px;
      opacity: 0;
      transition: opacity 0.3s ease;

      .el-icon {
        font-size: 14px;
      }
    }

    &:hover {
      .user-avatar {
        filter: brightness(0.9);
      }

      .avatar-upload {
        opacity: 1;
      }
    }
  }
}

// 带操作按钮的表单项
.form-item-with-action {
  display: flex;
  align-items: flex-end;
  gap: 12px;

  .form-item-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;

    .form-label {
      font-size: 14px;
      font-weight: 500;
      color: #333;

      .nickname-display {
        font-size: 12px;
        color: #1890ff;
        font-weight: 400;
      }
    }

    .form-input {
      width: 100%;
    }
  }

  .el-button {
    margin-bottom: 0;
    min-width: 80px;
  }
}

.form-section {
  background-color: #f7f8fa;
  border-radius: 12px;
  padding: 20px;
  margin-top: 8px;

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e5e7eb;
  }
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }

  .form-label {
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }

  .form-input {
    width: 100%;
  }

  .form-slider {
    padding: 0 8px;
  }

  .form-tip {
    font-size: 12px;
    color: #999;
    line-height: 1.4;
  }
}

.ai-params-actions {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.dialog-footer {
  padding: 16px 24px 24px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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
