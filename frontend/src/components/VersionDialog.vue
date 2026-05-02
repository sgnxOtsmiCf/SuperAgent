<template>
  <div class="version-dialog-overlay" @click="handleClose">
    <div class="version-dialog" @click.stop>
      <div class="dialog-header">
        <h3>版本信息</h3>
        <el-icon class="close-btn" @click="handleClose"><Close /></el-icon>
      </div>

      <div class="dialog-content">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <!-- 版本详情 -->
        <template v-else-if="detailInfo.version">
          <!-- 基本信息 -->
          <div class="info-section">
            <div class="info-item">
              <span class="label">版本号：</span>
              <span class="value version-badge">{{ 'v'+detailInfo.version.version || 'v1.0' }}</span>
            </div>
            <div v-if="detailInfo.version.author" class="info-item">
              <span class="label">作者：</span>
              <span class="value">{{ detailInfo.version.author }}</span>
            </div>
            <div v-if="detailInfo.version.description" class="info-item">
              <span class="label">描述：</span>
              <span class="value">{{ detailInfo.version.description }}</span>
            </div>
          </div>

          <!-- 优点列表 -->
          <div v-if="hasAdvantages" class="info-section">
            <h4 class="section-title advantage">
              <el-icon><StarFilled /></el-icon>
              核心优势
            </h4>
            <div class="feature-list">
              <div
                v-for="(desc, title) in detailInfo.advantages"
                :key="title"
                class="feature-item advantage"
              >
                <div class="feature-title">{{ title }}</div>
                <div class="feature-desc">{{ desc }}</div>
              </div>
            </div>
          </div>

          <!-- 不足列表 -->
          <div v-if="hasDeficiencies" class="info-section">
            <h4 class="section-title deficiency">
              <el-icon><WarningFilled /></el-icon>
              待改进项
            </h4>
            <div class="feature-list">
              <div
                v-for="(desc, title) in detailInfo.deficiencies"
                :key="title"
                class="feature-item deficiency"
              >
                <div class="feature-title">{{ title }}</div>
                <div class="feature-desc">{{ desc }}</div>
              </div>
            </div>
          </div>
        </template>

        <!-- 无数据状态 -->
        <div v-else class="empty-state">
          <el-icon :size="48" color="#ccc"><InfoFilled /></el-icon>
          <p>暂无版本信息</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { versionApi } from '@/api/version'
import { Close, StarFilled, WarningFilled, InfoFilled, Loading } from '@element-plus/icons-vue'
import { logger } from '@/utils/logger'

const props = defineProps({
  versionInfo: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['close'])

const loading = ref(false)
const detailInfo = ref({
  version: null,
  advantages: {},
  deficiencies: {}
})

const hasAdvantages = computed(() => {
  return Object.keys(detailInfo.value.advantages || {}).length > 0
})

const hasDeficiencies = computed(() => {
  return Object.keys(detailInfo.value.deficiencies || {}).length > 0
})

async function loadVersionDetail() {
  loading.value = true
  try {
    const res = await versionApi.getVersionDetail()
    if (res.code === 200 && res.data) {
      detailInfo.value = {
        version: res.data.version || props.versionInfo,
        advantages: res.data.advantages || {},
        deficiencies: res.data.deficiencies || {}
      }
    }
  } catch (error) {
    logger.error('[VersionDialog] 加载版本详情失败:', error)
 // 使用传入的基本信息作为回退
    detailInfo.value.version = props.versionInfo
  } finally {
    loading.value = false
  }
}

function handleClose() {
  emit('close')
}

onMounted(() => {
  loadVersionDetail()
})
</script>

<style lang="scss" scoped>
.version-dialog-overlay {
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
  animation: fadeIn 0.2s ease;
}

.version-dialog {
  background-color: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.3s ease;
}

.dialog-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #1a1a1a;
  }

  .close-btn {
    font-size: 20px;
    color: #999;
    cursor: pointer;
    padding: 4px;
    border-radius: 6px;
    transition: all 0.2s;

    &:hover {
      background-color: #f5f5f5;
      color: #666;
    }
  }
}

.dialog-content {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
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
    font-size: 24px;
    animation: rotate 1s linear infinite;
  }
}

.info-section {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
  line-height: 1.6;

  &:last-child {
    margin-bottom: 0;
  }

  .label {
    color: #666;
    font-size: 14px;
    flex-shrink: 0;
  }

  .value {
    color: #333;
    font-size: 14px;
    flex: 1;
  }

  .version-badge {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    padding: 2px 10px;
    border-radius: 12px;
    font-weight: 600;
    font-size: 13px;
  }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid;

  &.advantage {
    color: #52c41a;
    border-color: #d9f7be;

    .el-icon {
      color: #52c41a;
    }
  }

  &.deficiency {
    color: #fa8c16;
    border-color: #ffe7ba;

    .el-icon {
      color: #fa8c16;
    }
  }
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-item {
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid;

  &.advantage {
    background-color: #f6ffed;
    border-color: #52c41a;
  }

  &.deficiency {
    background-color: #fff7e6;
    border-color: #fa8c16;
  }

  .feature-title {
    font-size: 14px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 8px;
  }

  .feature-desc {
    font-size: 13px;
    color: #666;
    line-height: 1.6;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #999;
  gap: 12px;

  p {
    margin: 0;
    font-size: 14px;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
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
</style>
