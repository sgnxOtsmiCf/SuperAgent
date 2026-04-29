<template>
  <div class="param-overlay" @click.self="$emit('close')">
    <div class="param-dialog">
      <div class="dialog-header">
        <h3>参数设置 - {{ modelName }}</h3>
        <el-button text circle :icon="Close" @click="$emit('close')" />
      </div>

      <div class="param-content">
        <div v-if="!model" class="loading-state">
          <span>加载中...</span>
        </div>

        <template v-else>
          <div class="param-group">
            <div class="param-label">
              <span>温度 (Temperature)</span>
              <span class="param-value">{{ localParams.temperature }}</span>
            </div>
            <el-slider
              v-model="localParams.temperature"
              :min="getConstraintMin('temperature')"
              :max="getConstraintMax('temperature')"
              :step="0.1"
              :show-tooltip="false"
              :disabled="!hasConstraint('temperature')"
            />
            <div class="param-range">
              <span>{{ getConstraintMin('temperature') }}</span>
              <span>{{ getConstraintMax('temperature') }}</span>
            </div>
            <p class="param-hint" v-if="!hasConstraint('temperature')">此模型不支持温度参数调整</p>
          </div>

          <div class="param-group">
            <div class="param-label">
              <span>Top-P</span>
              <span class="param-value">{{ localParams.topP }}</span>
            </div>
            <el-slider
              v-model="localParams.topP"
              :min="getConstraintMin('topP')"
              :max="getConstraintMax('topP')"
              :step="0.05"
              :show-tooltip="false"
              :disabled="!hasConstraint('topP')"
            />
            <div class="param-range">
              <span>{{ getConstraintMin('topP') }}</span>
              <span>{{ getConstraintMax('topP') }}</span>
            </div>
            <p class="param-hint" v-if="!hasConstraint('topP')">此模型不支持Top-P参数调整</p>
          </div>

          <div class="param-group">
            <div class="param-label">
              <span>Top-K</span>
              <span class="param-value">{{ localParams.topK }}</span>
            </div>
            <el-slider
              v-model="localParams.topK"
              :min="getConstraintMin('topK')"
              :max="getConstraintMax('topK')"
              :step="1"
              :show-tooltip="false"
              :disabled="!hasConstraint('topK')"
            />
            <div class="param-range">
              <span>{{ getConstraintMin('topK') }}</span>
              <span>{{ getConstraintMax('topK') }}</span>
            </div>
            <p class="param-hint" v-if="!hasConstraint('topK')">此模型不支持Top-K参数调整</p>
          </div>

          <div class="param-group">
            <div class="param-label">
              <span>最大输出Token数 (Max Tokens)</span>
              <span class="param-value">{{ localParams.maxTokens }}</span>
            </div>
            <el-slider
              v-model="localParams.maxTokens"
              :min="getConstraintMin('maxTokens')"
              :max="getConstraintMax('maxTokens')"
              :step="100"
              :show-tooltip="false"
              :disabled="!hasConstraint('maxTokens')"
            />
            <div class="param-range">
              <span>{{ formatNumber(getConstraintMin('maxTokens')) }}</span>
              <span>{{ formatNumber(getConstraintMax('maxTokens')) }}</span>
            </div>
            <p class="param-hint" v-if="!hasConstraint('maxTokens')">此模型不支持最大Token数调整</p>
          </div>

          <div class="param-group">
            <div class="param-label">
              <span>思考预算 (Thinking Budget)</span>
            </div>
            <el-input-number
              v-model="localParams.thinkingBudget"
              :min="getConstraintMin('thinkingBudget')"
              :max="getConstraintMax('thinkingBudget')"
              :step="Math.max(1, Math.floor((getConstraintMax('thinkingBudget') - getConstraintMin('thinkingBudget')) / 100))"
              controls-position="right"
              class="param-number-input"
              :disabled="!hasConstraint('thinkingBudget')"
            />
            <p class="param-hint" v-if="!hasConstraint('thinkingBudget')">此模型不支持思考预算设置</p>
          </div>

          <div class="param-group switch-group" v-if="showEnableThinking">
            <div class="param-label">
              <span>深度思考</span>
            </div>
            <el-switch v-model="localParams.enableThinking" />
            <p class="param-hint">开启后模型将进行更深度的推理</p>
          </div>

          <div class="param-group switch-group" v-if="showEnableSearch">
            <div class="param-label">
              <span>联网搜索</span>
            </div>
            <el-switch v-model="localParams.enableSearch" />
            <p class="param-hint">开启后模型将联网搜索最新信息</p>
          </div>
        </template>
      </div>

      <div class="dialog-footer">
        <el-button @click="handleReset">重置为默认</el-button>
        <el-button type="primary" @click="handleSave">保存设置</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelId: { type: String, required: true },
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['close'])

const chatStore = useChatStore()

const model = computed(() => chatStore.modelList.find(m => m.id === props.modelId))
const modelName = computed(() => model.value?.name || props.modelId)

const constraints = computed(() => model.value?.paramConstraints || {})

const localParams = ref({
  temperature: 0.75,
  topP: 0.9,
  topK: 10,
  maxTokens: 200000,
  thinkingBudget: 0,
  enableThinking: false,
  enableSearch: false
})

function initLocalParams() {
  if (props.modelId) {
    const effective = chatStore.getEffectiveParams(props.modelId)
    localParams.value = { ...effective }
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    initLocalParams()
  }
})

watch(model, (newModel) => {
  if (newModel && props.visible) {
    initLocalParams()
  }
})

onMounted(() => {
  if (props.visible) {
    initLocalParams()
  }
})

function hasConstraint(key) {
  const c = constraints.value[key]
  if (key === 'enableThinking' || key === 'enableSearch') {
    return c != null
  }
  return c && typeof c === 'object' && c.min != null && c.max != null
}

function getConstraintMin(key) {
  const c = constraints.value[key]
  if (c && typeof c === 'object' && c.min != null) return Number(c.min)
  const defaults = { temperature: 0, topP: 0, topK: 0, maxTokens: 100, thinkingBudget: 0 }
  return defaults[key] || 0
}

function getConstraintMax(key) {
  const c = constraints.value[key]
  if (c && typeof c === 'object' && c.max != null) return Number(c.max)
  const defaults = { temperature: 2, topP: 1, topK: 100, maxTokens: 200000, thinkingBudget: 128000 }
  return defaults[key] || 1
}

const showEnableThinking = computed(() => {
  return hasConstraint('enableThinking') || localParams.value.enableThinking != null
})

const showEnableSearch = computed(() => {
  return hasConstraint('enableSearch') || localParams.value.enableSearch != null
})

function formatNumber(num) {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return String(num)
}

function handleReset() {
  const resetMap = { ...chatStore.modelParams }
  delete resetMap[props.modelId]
  chatStore.modelParams = resetMap
  localParams.value = chatStore.getEffectiveParams(props.modelId)
  ElMessage.info('已重置为默认配置')
}

function handleSave() {
  chatStore.setModelParams(props.modelId, {
    temperature: localParams.value.temperature,
    topP: localParams.value.topP,
    topK: localParams.value.topK,
    maxTokens: localParams.value.maxTokens,
    thinkingBudget: localParams.value.thinkingBudget,
    enableThinking: localParams.value.enableThinking ?? undefined,
    enableSearch: localParams.value.enableSearch ?? undefined
  })
  ElMessage.success(`${modelName.value} 参数已保存`)
  emit('close')
}
</script>

<style lang="scss" scoped>
.param-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
}

.param-dialog {
  background-color: #fff;
  border-radius: 16px;
  width: 520px;
  max-width: 90vw;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.18);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px 16px;
  flex-shrink: 0;

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }
}

.param-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 28px 8px;
}

.loading-state {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.param-group {
  margin-bottom: 24px;
}

.param-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.param-value {
  font-size: 13px;
  color: #0958d9;
  font-weight: 600;
  background: #f0f9ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.param-range {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #bbb;
  margin-top: 4px;
}

.param-hint {
  font-size: 12px;
  color: #ccc;
  margin: 4px 0 0;
  font-style: italic;
}

.switch-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  .param-label {
    margin-bottom: 0;
  }

  .param-hint {
    width: 100%;
    flex-basis: 100%;
  }
}

.param-number-input {
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 28px 24px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

:deep(.el-slider.is-disabled .el-slider__bar) {
  background-color: #e5e7eb;
}

:deep(.el-slider.is-disabled .el-slider__button) {
  border-color: #d0d0d0;
}
</style>
