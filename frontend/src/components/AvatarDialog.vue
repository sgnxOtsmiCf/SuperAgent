<template>
  <div class="avatar-overlay" @click.self="$emit('close')">
    <div class="avatar-dialog">
      <div class="dialog-header">
        <h3>设置头像</h3>
        <el-button text circle :icon="Close" @click="$emit('close')" />
      </div>

      <div class="avatar-preview">
        <el-avatar :size="120" :src="previewUrl || userStore.userInfo.avatar || ''">
          <el-icon :size="40"><UserFilled /></el-icon>
        </el-avatar>
      </div>

      <div class="upload-section">
        <el-upload
          ref="uploadRef"
          action=""
          :auto-upload="false"
          :show-file-list="false"
          accept="image/*"
          :on-change="handleFileChange"
        >
          <template #trigger>
            <el-button type="primary" size="large" class="upload-btn">
              <el-icon><Upload /></el-icon>
              选择图片
            </el-button>
          </template>
        </el-upload>
        
        <p class="tip">支持 JPG、PNG、GIF 格式，建议尺寸不小于 200x200</p>
      </div>

      <div class="action-buttons">
        <el-button size="large" @click="$emit('close')">取消</el-button>
        <el-button 
          type="primary" 
          size="large" 
          :loading="isLoading"
          :disabled="!selectedFile"
          @click="handleSave"
        >
          保存
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Close, UserFilled, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'

const emit = defineEmits(['close', 'success'])
const userStore = useUserStore()

const uploadRef = ref()
const selectedFile = ref(null)
const previewUrl = ref('')
const isLoading = ref(false)

function handleFileChange(file) {
  if (file) {
    selectedFile.value = file.raw

    // 创建预览URL
    const reader = new FileReader()
    reader.onload = (e) => {
      previewUrl.value = e.target.result
    }
    reader.readAsDataURL(file.raw)
  }
}

async function handleSave() {
  if (!selectedFile.value) return

  isLoading.value = true

  try {
    // 🔑🔑🔑 调用后端API上传头像到minio
    const res = await userApi.fileUpload(selectedFile.value)

    // 后端返回的是minio图片地址
    const avatarUrl = res.data

    // 更新用户信息（保存minio地址）
    userStore.setUserInfo({
      ...userStore.userInfo,
      avatar: avatarUrl
    })

    ElMessage.success('头像设置成功！')
    emit('success', avatarUrl)
    emit('close')

  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败，请重试')
  } finally {
    isLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.avatar-overlay {
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

.avatar-dialog {
  background-color: #fff;
  border-radius: 16px;
  width: 420px;
  max-width: 90vw;
  padding: 32px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;

  h3 {
    font-size: 24px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }
}

.avatar-preview {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;

  .el-avatar {
    border: 4px solid #f0f0f0;
    transition: all 0.3s;

    &:hover {
      border-color: #1890ff;
      transform: scale(1.05);
    }
  }
}

.upload-section {
  text-align: center;
  margin-bottom: 24px;

  .upload-btn {
    width: 200px;
    height: 44px;
    font-size: 15px;
  }

  .tip {
    margin-top: 12px;
    font-size: 13px;
    color: #999;
  }
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;

  .el-button {
    width: 100px;
  }
}
</style>
