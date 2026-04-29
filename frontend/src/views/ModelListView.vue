<template>
  <div class="model-list-view">
    <div class="mlv-header">
      <el-button text :icon="ArrowLeft" @click="goBack" class="back-btn">返回</el-button>
      <h2>模型列表</h2>
    </div>

    <div class="filter-bar">
      <el-select v-model="filters.groupId" placeholder="按分组筛选" clearable class="filter-item" @change="handleFilterChange">
        <el-option
          v-for="g in groups"
          :key="g.id"
          :label="g.groupName"
          :value="g.id"
        />
      </el-select>
      <el-select v-model="filters.providerId" placeholder="按厂商筛选" clearable class="filter-item" @change="handleFilterChange">
        <el-option
          v-for="p in providers"
          :key="p.id"
          :label="p.providerName"
          :value="p.id"
        />
      </el-select>
      <el-select v-model="filters.modelType" placeholder="按模型类型筛选" clearable class="filter-item" @change="handleFilterChange">
        <el-option label="LLM" value="llm" />
        <el-option label="Embedding" value="embedding" />
        <el-option label="Image" value="image" />
        <el-option label="Audio" value="audio" />
        <el-option label="Multimodal" value="multimodal" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="搜索模型名称/编码"
        clearable
        class="filter-input"
        @clear="fetchModels"
        @keyup.enter="fetchModels"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <div v-else-if="filteredModels.length === 0" class="empty-state">
      <el-empty description="暂无模型数据" />
    </div>

    <div v-else class="model-grid">
      <div
        v-for="model in filteredModels"
        :key="model.id"
        class="model-card"
      >
        <div class="model-card-header">
          <div class="model-name-row">
            <span class="model-name">{{ model.modelName }}</span>
            <div class="model-tags">
              <el-tag v-for="tag in parseTags(model.tags)" :key="tag" size="small" type="warning" effect="dark">
                {{ tag }}
              </el-tag>
            </div>
          </div>
          <el-tag size="small" effect="plain" :type="model.isRecommended ? 'success' : 'info'">
            {{ model.isRecommended ? '推荐' : '普通' }}
          </el-tag>
        </div>
        <div class="model-card-body">
          <div class="model-info-row">
            <span class="info-label">编码</span>
            <span class="info-value code-text">{{ model.modelCode }}</span>
          </div>
          <div class="model-info-row">
            <span class="info-label">厂商</span>
            <span class="info-value">{{ model.providerName }}</span>
          </div>
          <div class="model-info-row">
            <span class="info-label">类型</span>
            <el-tag size="small">{{ model.modelType }}</el-tag>
          </div>
          <div class="model-info-row">
            <span class="info-label">上下文窗口</span>
            <span class="info-value">{{ model.contextWindow ? model.contextWindow.toLocaleString() + ' tokens' : '-' }}</span>
          </div>
          <div class="model-info-row">
            <span class="info-label">最大输出</span>
            <span class="info-value">{{ model.maxOutputTokens ? model.maxOutputTokens.toLocaleString() + ' tokens' : '-' }}</span>
          </div>
          <div class="model-info-row pricing-row">
            <span class="info-label">输入价格</span>
            <span class="info-value price">¥{{ model.inputPricePer1M }}/百万tokens</span>
            <span class="info-label">输出价格</span>
            <span class="info-value price">¥{{ model.outputPricePer1M }}/百万tokens</span>
          </div>
          <div v-if="model.capabilities" class="model-info-row">
            <span class="info-label">能力</span>
            <div class="capability-tags">
              <el-tag
                v-for="cap in parseTags(model.capabilities)"
                :key="cap"
                size="small"
                type="primary"
                effect="plain"
              >
                {{ cap }}
              </el-tag>
            </div>
          </div>
        </div>
        <div v-if="model.description" class="model-card-footer">
          <p class="model-desc">{{ model.description }}</p>
        </div>
      </div>
    </div>

    <div class="pagination-wrapper" v-if="!loading">
      <el-pagination
        v-model:current-page="pagination.pageNo"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[8, 12, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Search, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { modelApi } from '@/api/model'

const router = useRouter()
const loading = ref(false)
const filteredModels = ref([])
const providers = ref([])
const groups = ref([])

const pagination = reactive({
  pageNo: 1,
  pageSize: 8,
  total: 0
})

const filters = reactive({
  groupId: null,
  providerId: null,
  modelType: '',
  keyword: ''
})

function goBack() {
  router.push('/')
}

async function fetchModels() {
  loading.value = true
  try {
    const params = {
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
    }
    if (filters.groupId) params.groupId = filters.groupId
    if (filters.providerId) params.providerId = filters.providerId
    if (filters.modelType) params.modelType = filters.modelType
    if (filters.keyword) params.keyword = filters.keyword
    const res = await modelApi.getModelList(params)
    if (res.code === 200 && res.data) {
      filteredModels.value = res.data.records || res.data || []
      pagination.total = res.data.total || 0
    } else {
      filteredModels.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('获取模型列表失败:', error)
    ElMessage.error('获取模型列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNo = 1
  fetchModels()
}

function handleFilterChange() {
  pagination.pageNo = 1
  fetchModels()
}

function handlePageChange(page) {
  pagination.pageNo = page
  fetchModels()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.pageNo = 1
  fetchModels()
}

async function fetchProviders() {
  try {
    const res = await modelApi.getModelProviders({ pageNo: 1, pageSize: 8 })
    if (res.code === 200 && res.data) {
      providers.value = res.data.records || res.data || []
    }
  } catch (error) {
    console.error('获取供应商列表失败:', error)
  }
}

async function fetchGroups() {
  try {
    const res = await modelApi.getModelGroups({ pageNo: 1, pageSize: 8 })
    if (res.code === 200 && res.data) {
      groups.value = res.data.records || res.data || []
    }
  } catch (error) {
    console.error('获取分组列表失败:', error)
  }
}

function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  try {
    const parsed = JSON.parse(tags)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

onMounted(() => {
  fetchModels()
  fetchProviders()
  fetchGroups()
})
</script>

<style lang="scss" scoped>
.model-list-view {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px 24px;
  overflow-y: auto;
  background-color: #f5f7fa;
}

.mlv-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;

  .back-btn {
    font-size: 14px;
  }

  h2 {
    font-size: 22px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;

  .filter-item {
    width: 160px;
  }

  .filter-input {
    width: 220px;
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
  gap: 12px;
  color: #999;

  .is-loading {
    animation: rotate 1s linear infinite;
  }
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
  flex: 1;
  align-content: start;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px 0 8px;
  margin-top: auto;
}

.model-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s ease;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  }
}

.model-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;

  .model-name-row {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .model-name {
      font-size: 16px;
      font-weight: 600;
      color: #1a1a1a;
    }

    .model-tags {
      display: flex;
      gap: 4px;
      flex-wrap: wrap;
    }
  }
}

.model-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.model-info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;

  .info-label {
    color: #999;
    min-width: 70px;
  }

  .info-value {
    color: #333;
  }

  .code-text {
    font-family: monospace;
    background-color: #f5f5f5;
    padding: 1px 6px;
    border-radius: 4px;
  }
}

.pricing-row {
  .price {
    color: #e6a23c;
    font-weight: 500;
    margin-right: 16px;
  }
}

.capability-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.model-card-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;

  .model-desc {
    font-size: 12px;
    color: #808080;
    line-height: 1.5;
    margin: 0;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
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
