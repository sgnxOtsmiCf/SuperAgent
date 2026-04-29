<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <el-icon :size="24"><MagicStick /></el-icon>
      <span class="logo-text">SuperAgent</span>
      <!-- 🔑 版本信息标签 -->
      <span class="version-tag" @click="showVersionDialog = true" title="点击查看版本详情">
        {{ 'v'+(versionInfo.version || 'v1.0') }}
      </span>
    </div>

    <div class="new-chat-btn" @click="handleNewChat">
      <el-icon><Plus /></el-icon>
      <span>新建对话</span>
    </div>

    <nav class="app-nav">
      <div
        v-for="(app, key) in appList"
        :key="key"
        class="app-item"
        :class="{ active: currentApp === key }"
        @click="switchToApp(key)"
      >
        <el-icon :size="20">
          <component :is="app.icon" />
        </el-icon>
        <span>{{ app.name }}</span>
      </div>
    </nav>

    <div class="model-list-entry" @click="router.push('/model-list')">
      <el-icon :size="20"><List /></el-icon>
      <span>模型列表</span>
    </div>

    <div class="history-section">
      <div class="history-header">
        <el-icon><Clock /></el-icon>
        <span>历史对话</span>
        <!-- 刷新按钮 -->
        <el-icon 
          v-if="userStore.isLoggedIn" 
          class="refresh-btn" 
          :class="{ loading: chatStore.isLoadingSessions }"
          @click.stop="refreshHistory"
        >
          <Refresh />
        </el-icon>
        <!-- 归档入口按钮 -->
        <el-icon 
          v-if="userStore.isLoggedIn" 
          class="archive-btn" 
          title="查看归档"
          @click.stop="showArchiveDialog = true"
        >
          <Files />
        </el-icon>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="chatStore.isLoadingSessions" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      
      <div v-else class="history-list">
        <div
          v-for="(session, index) in displayHistory"
          :key="session.sessionId"
          class="history-item"
          :class="{ 
            'is-pinned': session.isPinned,
            'is-active': session.sessionId === chatStore.currentSessionId[chatStore.currentApp]
          }"
          @click="loadSession(session)"
        >
          <!-- 置顶图标（替换聊天图标） -->
          <el-icon v-if="session.isPinned" class="pin-icon"><Top /></el-icon>
          <el-icon v-else><ChatDotRound /></el-icon>
          
          <!-- 标题 -->
          <span class="history-title">{{ session.title || session.sessionName || '新对话' }}</span>
          <!-- 归档标签 -->
          <el-tag v-if="session.isArchived" size="small" type="info" effect="plain" class="archive-tag">已归档</el-tag>
          
          <!-- "..." 更多操作按钮 -->
          <el-dropdown
            trigger="click"
            @command="(cmd) => handleSessionCommand(cmd, session)"
            @click.stop
            class="session-dropdown"
          >
            <el-icon class="more-btn" @click.stop><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">
                  <el-icon><Edit /></el-icon>
                  编辑标题
                </el-dropdown-item>
                <el-dropdown-item command="pin">
                  <el-icon><Top /></el-icon>
                  {{ session.isPinned ? '取消置顶' : '置顶' }}
                </el-dropdown-item>
                <!-- 归档会话不显示归档和删除选项 -->
                <template v-if="!session.isArchived">
                  <el-dropdown-item command="archive">
                    <el-icon><FolderChecked /></el-icon>
                    归档
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <el-icon style="color: #ff4d4f;"><Delete /></el-icon>
                    <span style="color: #ff4d4f;">删除</span>
                  </el-dropdown-item>
                </template>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        
        <!-- 查看全部按钮 -->
        <div 
          v-if="hasMoreHistory" 
          class="history-item view-all"
          @click="showHistoryDialog = true"
        >
          <el-icon><ArrowDown /></el-icon>
          <span class="view-all-text">查看全部</span>
        </div>

        <!-- 空状态提示 -->
        <div v-if="currentHistory.length === 0 && userStore.isLoggedIn" class="empty-history">
          <span>暂无历史对话</span>
        </div>
      </div>
    </div>

    <div class="sidebar-footer">
      <!-- 未登录状态 -->
      <div v-if="!userStore.isLoggedIn" class="user-info" @click="handleLoginClick">
        <el-avatar :size="36" :icon="UserFilled" />
        <span>登录/注册</span>
      </div>

      <!-- 已登录状态：参考图二样式 -->
      <div v-else class="user-section">
        <!-- 用户信息栏：头像 + 用户名 + 升级 + 箭头 -->
        <div class="user-info" @click="toggleUserMenu">
          <el-avatar 
            :size="36" 
            :src="userStore.userInfo.avatar || ''"
            class="user-avatar"
          >
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <span class="username">{{ userStore.userInfo.nickName || userStore.userInfo.username || '用户' }}</span>
          <span class="upgrade-btn" @click.stop="handleUpgradeClick">升级</span>
          <el-icon class="arrow" :class="{ expanded: showUserMenu }"><ArrowUp /></el-icon>
        </div>

        <!-- 用户菜单：参考图三样式 -->
        <transition name="slide-up">
          <div v-show="showUserMenu" class="user-menu">
            <div 
              v-for="item in menuItems" 
              :key="item.key"
              class="menu-item"
              @click="handleMenuClick(item.key)"
            >
              <el-icon :size="18">
                <component :is="item.icon" />
              </el-icon>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <!-- 头像设置对话框 -->
    <AvatarDialog 
      v-if="showAvatarDialog" 
      @close="showAvatarDialog = false" 
    />

    <!-- 用户基本信息对话框 -->
    <UserBasicInfoDialog 
      v-if="showUserBasicInfoDialog" 
      @close="showUserBasicInfoDialog = false"
      @user-updated="handleUserUpdated"
    />

    <!-- 历史会话对话框 -->
    <HistoryDialog 
      v-if="showHistoryDialog"
      :app-key="currentApp"
      @close="showHistoryDialog = false"
      @select-session="handleSelectSession"
    />

    <!-- 归档会话对话框 -->
    <ArchiveDialog
      v-if="showArchiveDialog"
      :app-key="currentApp"
      @close="showArchiveDialog = false"
      @select-session="handleSelectArchivedSession"
    />

    <!-- 工具/Skills 列表对话框 -->
    <FunctionListDialog
      v-if="showFunctionDialog"
      :type="functionDialogType"
      @close="showFunctionDialog = false"
    />

    <!-- 🔑 版本信息对话框 -->
    <VersionDialog
      v-if="showVersionDialog"
      :version-info="versionInfo"
      @close="showVersionDialog = false"
    />

    <!-- 用户画像对话框 -->
    <UserProfileDialog
      v-if="showUserProfileDialog"
      @close="showUserProfileDialog = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, inject, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import { versionApi } from '@/api/version'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MagicStick,
  Plus,
  Cpu,
  House,
  ChatDotRound,
  UserFilled,
  ArrowUp,
  ArrowDown,
  Clock,
  Avatar,
  SwitchButton,
  Message,
  Refresh,
  Delete,
  Loading,
  MoreFilled,
  Edit,
  Top,
  Tools,
  Medal,
  InfoFilled,
  Connection,
  Setting,
  FolderChecked,
  Files,
  List
} from '@element-plus/icons-vue'
import AvatarDialog from './AvatarDialog.vue'
import UserBasicInfoDialog from './UserBasicInfoDialog.vue'
import HistoryDialog from './HistoryDialog.vue'
import ArchiveDialog from './ArchiveDialog.vue'
import FunctionListDialog from './FunctionListDialog.vue'
import VersionDialog from './VersionDialog.vue'
import UserProfileDialog from './UserProfileDialog.vue'

const chatStore = useChatStore()
const userStore = useUserStore()
const router = useRouter()
const toggleLogin = inject('toggleLogin')

const showUserMenu = ref(false)
const showAvatarDialog = ref(false)
const showUserBasicInfoDialog = ref(false)
const showHistoryDialog = ref(false)
const showArchiveDialog = ref(false)
const showFunctionDialog = ref(false)
const showVersionDialog = ref(false)
const showUserProfileDialog = ref(false)
const functionDialogType = ref('tools')

// 🔑 版本信息
const versionInfo = ref({
  version: 'v1.0',
  author: '',
  description: '',
  advantages: {},
  deficiencies: {}
})

const appList = {
  superagent: { name: '超级智能体', icon: 'MagicStick' },
  manus: { name: 'OpenManus', icon: 'Cpu' },
  family: { name: '家庭和睦助手', icon: 'House' }
}

const menuItems = ref([
  { key: 'about', label: '用户基本信息', icon: 'InfoFilled' },
  { key: 'profile', label: '用户画像', icon: 'Connection' },
  { key: 'avatar', label: '头像设置', icon: 'Avatar' },
  { key: 'tools', label: 'Agent 工具列表', icon: 'Tools' },
  { key: 'skills', label: 'Skills 列表', icon: 'MagicStick' },
  { key: 'vip', label: '会员计划', icon: 'Medal' },
  { key: 'feedback', label: '用户反馈', icon: 'Message' },
  { key: 'settings', label: '设置', icon: 'Setting' },
  { key: 'logout', label: '退出登录', icon: 'SwitchButton' }
])

const currentApp = computed(() => chatStore.currentApp)
const currentHistory = computed(() => chatStore.getSessionHistory(chatStore.currentApp))

// 只显示前8条历史记录（后端已按置顶时间排序）
// 后端查询9条，如果返回9条说明有更多数据，需要显示"查看更多"
const displayHistory = computed(() => {
  const history = [...currentHistory.value]
  // 🔑 关键：后端已排序，前端只需截取前8条
  // 排序规则：置顶最新的 → 置顶较早的 → 未置顶但最近活跃的 → 未置顶且不活跃的
  return history.slice(0, 8)
})

// 🔑 是否有更多历史记录（后端返回9条说明有第9条，即还有更多）
const hasMoreHistory = computed(() => {
  return currentHistory.value.length > 8
})

function guardStreamingAction(message = '请先等待当前回复完成或停止接收') {
  if (!chatStore.isStreamingResponse) {
    return false
  }

  ElMessage.warning(message)
  return true
}

function switchToApp(appKey) {
  if (guardStreamingAction()) return
  chatStore.switchApp(appKey)
}

function handleNewChat() {
  if (guardStreamingAction()) return
  chatStore.createNewSession(chatStore.currentApp)
}

function loadSession(session) {
  if (guardStreamingAction()) return

  if (!session.sessionId) {
    console.error('[Sidebar] ❌ sessionId为空！')
    return
  }

  chatStore.loadSession(session.sessionId, chatStore.currentApp)
}

function handleSelectSession(session) {
  showHistoryDialog.value = false
  loadSession(session)
}

function handleSelectArchivedSession(session) {
  showArchiveDialog.value = false

  if (!session.sessionId) {
    console.error('[Sidebar] 归档会话缺少 sessionId')
    return
  }

  const appKey = chatStore.currentApp

  const sessionList = chatStore.sessions[appKey]
  if (!sessionList) {
    console.error(`[Sidebar] sessions[${appKey}] 不存在`)
    return
  }

  // 查找归档会话是否已在本地列表中
  let existingIndex = sessionList.findIndex(s => s.sessionId === session.sessionId)

  if (existingIndex !== -1) {
    // 已存在，更新消息内容为归档数据（含完整 messages）
    sessionList[existingIndex] = { ...sessionList[existingIndex], ...session, isArchived: true }
  } else {
    // 不在本地列表中，添加进去
    sessionList.unshift({ ...session, isArchived: true })
  }

  // 🔑 直接用 loadSession 渲染（loadSession 从 sessions 列表中读取 messages）
  // 不再调用 fetchSessionDetailFromBackend 去查 Redis（归档数据已不在 Redis 中）
  chatStore.loadSession(session.sessionId, appKey)
}

// 🔑 企业级：刷新历史会话（从后端加载）
async function refreshHistory() {
  if (guardStreamingAction('请先停止当前回复再刷新历史记录')) return
  if (chatStore.isLoadingSessions) return
  
  await chatStore.fetchSessionsFromBackend(chatStore.currentApp)
}

// 🔑 企业级：处理会话操作命令（编辑、置顶、删除）
function handleSessionCommand(command, session) {
  if (guardStreamingAction('请先停止当前回复再操作历史会话')) return

  switch (command) {
    case 'edit':
      handleEditSessionTitle(session)
      break
    case 'pin':
      handlePinSession(session)
      break
    case 'archive':
      handleArchiveSession(session)
      break
    case 'delete':
      handleDeleteSession(session)
      break
  }
}

// 编辑会话标题
const editingSessionId = ref(null)
const editingTitle = ref('')

function handleEditSessionTitle(session) {
  editingSessionId.value = session.sessionId
  editingTitle.value = session.title || session.sessionName || ''
  
  ElMessageBox.prompt('请输入新的标题', '编辑标题', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: editingTitle.value,
    inputPattern: /\S+/,
    inputErrorMessage: '标题不能为空'
  }).then(async ({ value }) => {
    const updated = await chatStore.updateSessionTitle(
      session.sessionId,
      value,
      chatStore.currentApp
    )

    if (updated) {
      ElMessage.success('标题已更新')
    }
  }).catch(() => {
    // 用户取消
  }).finally(() => {
    editingSessionId.value = null
  })
}

// 置顶/取消置顶
async function handlePinSession(session) {
  // 🔑 修复：先保存当前状态，再执行切换
  const wasPinned = session.isPinned

  // 调用store方法，获取返回值（现在是异步的）
  const success = await chatStore.togglePinSession(session.sessionId, chatStore.currentApp)

  if (success) {
    // 切换成功，显示对应消息
    ElMessage.success(wasPinned ? '已取消置顶' : '已置顶')
  }
  // 失败的情况在store中已经处理了提示
}

// 归档会话
async function handleArchiveSession(session) {
  try {
    await ElMessageBox.confirm(
      `确定要归档对话"${session.title || '新对话'}"吗？归档后该会话将从活跃列表移至归档区。`,
      '归档确认',
      {
        confirmButtonText: '归档',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    const success = await chatStore.archiveSession(session.sessionId, chatStore.currentApp)
    if (success) {
      // 归档成功后，从本地列表移除该会话
      const sessionList = chatStore.sessions[chatStore.currentApp]
      const index = sessionList.findIndex(s => s.sessionId === session.sessionId)
      if (index !== -1) {
        sessionList.splice(index, 1)
        // 如果归档的是当前会话，切换到最新会话
        if (chatStore.currentSessionId[chatStore.currentApp] === session.sessionId) {
          if (sessionList.length > 0) {
            chatStore.switchApp(chatStore.currentApp)
          } else {
            chatStore.currentSessionId[chatStore.currentApp] = null
            chatStore.currentMessages = []
          }
        }
      }
    }
  } catch (error) {
    // 用户取消操作
  }
}

// 🔑 企业级：删除会话
async function handleDeleteSession(session) {
  try {
    await ElMessageBox.confirm(
      `确定要删除对话"${session.title || '新对话'}"吗？`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await chatStore.deleteSession(session.sessionId, chatStore.currentApp)
    
  } catch (error) {
    // 用户取消操作
  }
}

function handleDocumentClick(event) {
  if (!showUserMenu.value) {
    return
  }

  const target = event.target
  if (!(target instanceof Element)) {
    return
  }

  const userSection = target.closest('.user-section')
  if (!userSection) {
    showUserMenu.value = false
  }
}

// 🔑 加载版本信息
async function loadVersionInfo() {
  try {
    const res = await versionApi.getVersion()
    if (res.code === 200 && res.data) {
      versionInfo.value = {
        version: res.data.version || 'v1.0',
        author: res.data.author || '',
        description: res.data.description || ''
      }
    }
  } catch (error) {
    console.error('[Sidebar] 加载版本信息失败:', error)
  }
}

// 🔑 组件挂载时，如果已登录则自动从后端加载历史会话
onMounted(async () => {
  document.addEventListener('click', handleDocumentClick)

  // 🔑 加载版本信息
  await loadVersionInfo()

  // 🔑🔑🔑 关键修复：先加载后端数据，再切换应用显示
  if (userStore.isLoggedIn) {
    await chatStore.fetchSessionsFromBackend(chatStore.currentApp)
  }

  // 数据加载完成后再切换应用显示当前会话
  chatStore.switchApp(chatStore.currentApp)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
})

// 监听登录状态变化，登录后自动加载
watch(() => userStore.isLoggedIn, async (isLoggedIn) => {
  if (isLoggedIn) {
    await chatStore.fetchSessionsFromBackend(chatStore.currentApp)
  }
})

// 监听应用切换，切换后加载对应应用的历史
watch(() => chatStore.currentApp, async (newApp) => {
  if (userStore.isLoggedIn) {
    await chatStore.fetchSessionsFromBackend(newApp)
  }
})

function handleLoginClick() {
  toggleLogin()
}

// 处理用户信息更新
function handleUserUpdated(updatedUserInfo) {
  // 用户信息已在对话框中更新到 store，这里可以做一些额外的处理
  console.log('[Sidebar] 用户信息已更新:', updatedUserInfo)
}

function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value
}

function handleUpgradeClick() {
  ElMessage.info('功能开发中...')
}

async function handleMenuClick(key) {
  showUserMenu.value = false

  switch (key) {
    case 'avatar':
      // 🔑 打开头像设置对话框
      showAvatarDialog.value = true
      break

    case 'tools':
      // 🔑 打开工具列表对话框
      functionDialogType.value = 'tools'
      showFunctionDialog.value = true
      break

    case 'skills':
      // 🔑 打开 Skills 列表对话框
      functionDialogType.value = 'skills'
      showFunctionDialog.value = true
      break

    case 'vip':
      ElMessage.info('会员计划功能开发中...')
      break

    case 'about':
      // 打开用户基本信息对话框
      showUserBasicInfoDialog.value = true
      break

    case 'profile':
      // 打开用户画像对话框
      showUserProfileDialog.value = true
      break

    case 'feedback':
      ElMessage.info('用户反馈功能开发中...')
      break

    case 'settings':
      ElMessage.info('设置功能开发中...')
      break

    case 'logout':
      if (guardStreamingAction('请先停止当前回复再退出登录')) return

      try {
        await ElMessageBox.confirm(
          '确定要退出登录吗？',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        try {
          await userApi.logout()
        } catch (logoutError) {
          console.warn('[Sidebar] 后端退出登录失败，继续清理本地状态:', logoutError)
        }

        userStore.logout()

        // 清除所有会话数据
        chatStore.clearAllSessions()
        ElMessage.success('已成功退出登录')
      } catch (error) {
        // 用户取消
      }
      break

    default:
      // 未知菜单项，静默处理
  }
}
</script>

<style lang="scss" scoped>
.sidebar {
  width: 260px;
  background-color: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  height: 100%;
  flex-shrink: 0;
  position: relative;
}

.sidebar-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #f0f0f0;

  .logo-text {
    font-size: 18px;
    font-weight: 600;
    color: #1a1a1a;
  }

  // 🔑 版本标签样式
  .version-tag {
    margin-left: auto;
    padding: 2px 8px;
    font-size: 11px;
    color: #1890ff;
    background-color: #e6f7ff;
    border: 1px solid #91d5ff;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s ease;
    font-weight: 500;

    &:hover {
      background-color: #1890ff;
      color: #fff;
      border-color: #1890ff;
    }
  }
}

.new-chat-btn {
  margin: 16px 20px;
  padding: 12px 16px;
  background-color: #f7f8fa;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;

  &:hover {
    background-color: #eef0f2;
  }

  span {
    font-size: 14px;
    color: #333;
  }
}

.model-list-entry {
  margin: 0 12px 4px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.2s;
  color: #666;

  &:hover {
    background-color: #f0f9ff;
    color: #1890ff;

    .el-icon {
      color: #1890ff;
    }
  }

  span {
    font-size: 14px;
  }
}

.app-nav {
  padding: 0 12px;
  flex: 1;
  overflow-y: auto;
}

.app-item {
  padding: 12px 16px;
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.2s;

  &:hover {
    background-color: #f7f8fa;
  }

  &.active {
    background-color: #e8f3ff;
    color: #1890ff;

    .el-icon {
      color: #1890ff;
    }
  }

  span {
    font-size: 14px;
    color: #333;
  }
}

.history-section {
  margin-top: auto;
  border-top: 1px solid #f0f0f0;
  max-height: 480px;
  overflow-y: auto;
}

.history-header {
  padding: 12px 20px 8px;
  font-size: 13px;
  color: #666;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;

  .refresh-btn {
    margin-left: auto;
    cursor: pointer;
    color: #999;
    font-size: 14px;
    transition: all 0.2s;

    &:hover {
      color: #1890ff;
    }

    &.loading {
      animation: rotate 1s linear infinite;
    }
  }

  .archive-btn {
    cursor: pointer;
    color: #999;
    font-size: 14px;
    transition: all 0.2s;
    margin-left: 4px;

    &:hover {
      color: #faad14;
    }
  }
}

.loading-state {
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999;
  font-size: 13px;
}

.history-list {
  padding: 0 12px 12px;
}

.history-item {
  padding: 10px 16px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;

  // 🔑🔑🔑 当前选中的会话（深黄色高亮）
  &.is-active {
    background-color: #fff3cd;  // 深黄色背景
    border-left: 3px solid #ffc107;  // 深黄色边框
    font-weight: 500;

    .history-title {
      color: #856404;  // 深色文字
      font-weight: 600;
    }

    .el-icon {
      color: #ffc107;  // 深黄色图标
    }
  }

  &.is-pinned {
    background-color: #fffbe6;
    border-left: 3px solid #f5e6a3;  // 淡黄色边框
    
    .pin-icon {
      color: #f5d76e;  // 淡黄色图标
    }
    
    &:hover {
      .pin-icon {
        color: #faad14;  // hover时深黄色
      }
    }
  }

  &:hover {
    background-color: #f7f8fa;

    .more-btn {
      visibility: visible;  // 🔑 hover时可见
      opacity: 1;
    }
  }

  .pin-icon {
    color: #f5d76e;  // 淡黄色（默认）
    font-size: 16px;
    transition: color 0.2s ease;
  }

  .history-title {
    font-size: 13px;
    color: #666;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
  }

  .archive-tag {
    font-size: 10px;
    height: 16px;
    line-height: 16px;
    padding: 0 4px;
    margin-left: 4px;
    flex-shrink: 0;
  }

  .session-dropdown {
    margin-left: auto;
    margin-right: 4px;

    .more-btn {
      visibility: visible;  // 🔑 始终可见
      opacity: 1;
      color: #999;
      font-size: 25px;
      padding: 6px;
      border-radius: 6px;
      transition: all 0.2s ease;
      cursor: pointer;

      &:hover {
        background-color: rgba(0, 0, 0, 0.06);
        color: #666;
        transform: scale(1.05);
      }

      &:active {
        transform: scale(0.95);
      }
    }
  }
}
.empty-history {
  padding: 20px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid #e5e7eb;
}

.user-section {
  position: relative;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 10px;  // 🔑 圆角更大
  transition: all 0.2s;

  &:hover {
    background-color: #f7f8fa;
  }

  .user-avatar {
    flex-shrink: 0;
    
    :deep(.el-avatar) {
      background-color: transparent;  // 如果有自定义头像
      border: 2px solid #e5e7eb;
    }
  }

  span {
    font-size: 14px;
    color: #333;
  }

  .username {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100px;  // 给升级按钮留空间
    font-weight: 500;
  }

  // 🔑 "升级"按钮样式（参考图二）
  .upgrade-btn {
    padding: 3px 10px;
    font-size: 12px;
    color: #666;
    border: 1px solid #d9d9d9;
    border-radius: 6px;
    white-space: nowrap;
    font-weight: 400;
    transition: all 0.2s ease;

    &:hover {
      color: #1890ff;
      border-color: #1890ff;
      background-color: rgba(24, 144, 255, 0.04);
    }
  }

  .arrow {
    transition: transform 0.3s ease;
    color: #999;
    font-size: 14px;
    margin-left: auto;  // 靠右

    &.expanded {
      transform: rotate(180deg);
    }
  }
}

// 🔑 用户下拉菜单（参考图三样式）
.user-menu {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 12px;   // 🔑 左右留边距
  right: 12px;
  background-color: #fff;
  border-radius: 12px;  // 🔑 更大圆角
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);  // 🔑 更深阴影
  border: 1px solid #e5e7eb;
  overflow: hidden;
  z-index: 100;
  padding: 6px 0;  // 内部上下间距
  
  .menu-item {
    display: flex;
    align-items: center;
    gap: 14px;     // 图标和文字间距增大
    padding: 14px 18px;  // 内边距增大
    cursor: pointer;
    transition: all 0.2s;
    
    &:first-child {
      border-radius: 11px 11px 0 0;
    }
    
    &:last-child {
      border-radius: 0 0 11px 11px;
    }
    
    &:hover {
      background-color: #f5f5f5;
      
      &:only-child {
        border-radius: 11px;
      }
      
      span {
        color: #1890ff;  // hover时文字变蓝
      }
    }
    
    span {
      font-size: 14px;
      color: #333;
      flex: 1;
      font-weight: 400;
    }
    
    .el-icon {
      color: #666;
      font-size: 18px;  // 图标稍大
      width: 20px;       // 固定宽度，对齐
    }
  }
}

// 上滑动画
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.25s ease;
  transform-origin: bottom;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.95);
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
