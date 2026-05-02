<template>
  <div class="contact-overlay" @click.self="$emit('close')">
    <div class="contact-dialog">
      <div class="dialog-header">
        <h3>联系我们</h3>
        <el-button text circle :icon="Close" @click="$emit('close')" />
      </div>

      <div class="contact-content">
        <div class="icon-wrapper">
          <el-icon :size="48" color="#1890ff"><Message /></el-icon>
        </div>
        
        <h4>有任何问题或建议？</h4>
        <p class="description">我们非常重视您的反馈，请通过以下方式联系我们：</p>
        
        <div class="contact-info">
          <div class="info-item">
            <el-icon><Message /></el-icon>
            <span class="label">电子邮箱：</span>
            <a href="mailto:lixiangzhenshuaiqi@163.com" class="email">
              lixiangzhenshuaiqi@163.com
            </a>
            <el-button 
              text 
              type="primary" 
              size="small"
              @click="copyEmail"
            >
              {{ copied ? '已复制' : '复制' }}
            </el-button>
          </div>
        </div>

        <div class="response-time">
          <el-icon><Clock /></el-icon>
          <span>通常会在24小时内回复</span>
        </div>
      </div>

      <div class="action-buttons">
        <el-button type="primary" size="large" @click="$emit('close')">
          我知道了
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Close, Message, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { logger } from '@/utils/logger'

defineEmits(['close'])

const copied = ref(false)

async function copyEmail() {
  const email = 'lixiangzhenshuaiqi@163.com'
  
  try {
    await navigator.clipboard.writeText(email)
    copied.value = true
    ElMessage.success('邮箱地址已复制到剪贴板')
    
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (error) {
    logger.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style lang="scss" scoped>
.contact-overlay {
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

.contact-dialog {
  background-color: #fff;
  border-radius: 16px;
  width: 480px;
  max-width: 90vw;
  padding: 32px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;

  h3 {
    font-size: 24px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }
}

.contact-content {
  text-align: center;
}

.icon-wrapper {
  margin-bottom: 20px;

  .el-icon {
    animation: pulse 2s infinite;
  }
}

h4 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.description {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 24px;
}

.contact-info {
  background-color: #f7f8fa;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;

  .info-item {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    justify-content: center;

    .el-icon {
      color: #1890ff;
      font-size: 18px;
    }

    .label {
      font-size: 14px;
      color: #666;
      font-weight: 500;
    }

    .email {
      font-size: 15px;
      color: #1890ff;
      text-decoration: none;
      font-weight: 500;
      
      &:hover {
        text-decoration: underline;
      }
    }

    .el-button {
      margin-left: 8px;
    }
  }
}

.response-time {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: #999;

  .el-icon {
    font-size: 14px;
  }
}

.action-buttons {
  margin-top: 28px;

  .el-button {
    width: 100%;
    height: 44px;
    font-size: 15px;
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}
</style>
