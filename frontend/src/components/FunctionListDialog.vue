<template>
  <div class="function-overlay" @click.self="$emit('close')">
    <div class="function-dialog">
      <div class="dialog-header">
        <h3>{{ title }}</h3>
        <el-button text circle :icon="Close" @click="$emit('close')" />
      </div>

      <!-- Skills 提示 -->
      <div v-if="type === 'skills'" class="skills-notice">
        <el-icon><InfoFilled /></el-icon>
        <span>目前仅仅 SuperAgent 超级智能体支持 Skills 功能</span>
      </div>

      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <!-- 功能列表 -->
      <div v-else class="function-list">
        <div
          v-for="(item, index) in functionList"
          :key="index"
          class="function-item"
        >
          <div class="function-icon">
            <el-icon :size="24">
              <component :is="getIcon(item.type)" />
            </el-icon>
          </div>
          <div class="function-info">
            <h4>{{ item.name }}</h4>
            <p>{{ item.description }}</p>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="functionList.length === 0" class="empty-state">
          <el-icon :size="48" color="#d0d0d0"><Box /></el-icon>
          <p>暂无数据</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Close, Loading, InfoFilled, Box, Tools, MagicStick } from '@element-plus/icons-vue'
import { functionApi } from '@/api/function'
import { ElMessage } from 'element-plus'

const props = defineProps({
  type: {
    type: String,
    required: true,
    validator: (value) => ['tools', 'skills'].includes(value)
  }
})

const emit = defineEmits(['close'])

const isLoading = ref(false)
const functionList = ref([])

const title = props.type === 'tools' ? 'Agent 工具列表' : 'Skills 列表'

function getIcon(type) {
  return type === 'tool' ? Tools : MagicStick
}

async function loadData() {
  isLoading.value = true
  try {
    const res = props.type === 'tools'
      ? await functionApi.getTools()
      : await functionApi.getSkills()

    if (res.code === 200 && Array.isArray(res.data)) {
      functionList.value = res.data.map(item => ({
        name: item.name || '未命名',
        description: item.description || '暂无描述',
        type: item.type || props.type
      }))
    } else {
      functionList.value = []
    }
  } catch (error) {
    console.error(`[FunctionListDialog] 加载${props.type}列表失败:`, error)
    ElMessage.error(`加载${props.type === 'tools' ? '工具' : 'Skills'}列表失败`)
    functionList.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.function-overlay {
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

.function-dialog {
  background-color: #fff;
  border-radius: 16px;
  width: 500px;
  max-width: 90vw;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.dialog-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }
}

.skills-notice {
  margin: 16px 20px 0;
  padding: 12px 16px;
  background-color: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #ad8b00;

  .el-icon {
    color: #faad14;
    font-size: 16px;
  }
}

.loading-state {
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #999;

  .is-loading {
    animation: rotate 1s linear infinite;
  }
}

.function-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px 20px;
}

.function-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  margin-bottom: 12px;
  transition: all 0.2s;

  &:hover {
    border-color: #1890ff;
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
  }

  .function-icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .el-icon {
      color: #fff;
    }
  }

  .function-info {
    flex: 1;
    min-width: 0;

    h4 {
      font-size: 15px;
      font-weight: 600;
      color: #1a1a1a;
      margin: 0 0 6px 0;
    }

    p {
      font-size: 13px;
      color: #666;
      margin: 0;
      line-height: 1.5;
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;

  p {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}
</style>
