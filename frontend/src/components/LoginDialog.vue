<template>
  <div class="login-overlay" @click.self="$emit('close')">
    <div class="login-dialog">
      <div class="dialog-header">
        <h3>{{ isLogin ? '登录' : '注册' }}</h3>
        <el-button text circle :icon="Close" @click="$emit('close')" />
      </div>

      <el-tabs v-model="loginType" class="login-tabs">
        <!-- ==================== 账号密码 - 登录 ==================== -->
        <el-tab-pane label="账号密码" name="password">
          <!-- 登录表单 -->
          <el-form v-if="isLogin" ref="loginFormRef" :model="loginFormData" :rules="loginRules" @submit.prevent>
            <el-form-item prop="username">
              <el-input
                v-model="loginFormData.username"
                placeholder="请输入用户名"
                prefix-icon="User"
                size="large"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginFormData.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                show-password
                size="large"
              />
            </el-form-item>

            <el-form-item prop="captchaCode">
              <div class="captcha-input-wrapper">
                <el-input
                  v-model="loginFormData.captchaCode"
                  placeholder="请输入验证码"
                  prefix-icon="Key"
                  size="large"
                  @keyup.enter="handleLogin"
                />
                <div class="captcha-image" @click="refreshCaptcha">
                  <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                  <span v-else class="captcha-placeholder">点击获取</span>
                </div>
              </div>
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              @click="handleLogin"
              :loading="isLoading"
            >
              登录
            </el-button>
          </el-form>

          <!-- 注册表单 -->
          <el-form v-else ref="registerFormRef" :model="registerFormData" :rules="registerRules" @submit.prevent>
            <el-form-item prop="username">
              <el-input
                v-model="registerFormData.username"
                placeholder="请输入用户名"
                prefix-icon="User"
                size="large"
                @blur="checkUsername"
                @input="handleUsernameInput"
              >
                <template #suffix>
                  <el-icon v-if="isCheckingUsername" class="is-loading"><Loading /></el-icon>
                  <el-icon v-else-if="usernameChecked && !usernameExists" style="color: #67c23a"><CircleCheck /></el-icon>
                  <el-icon v-else-if="usernameChecked && usernameExists" style="color: #f56c6c"><CircleClose /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="registerFormData.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                show-password
                size="large"
              />
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              @click="handleRegister"
              :loading="isLoading"
            >
              注册
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- ==================== 手机号登录 ==================== -->
        <el-tab-pane label="手机号" name="phone">
          <el-form ref="phoneFormRef" :model="phoneFormData" :rules="phoneRules">
            <el-form-item prop="phone">
              <el-input
                v-model="phoneFormData.phone"
                placeholder="请输入手机号"
                prefix-icon="Phone"
                size="large"
              />
            </el-form-item>

            <el-form-item prop="verifyCode">
              <div class="code-input-wrapper">
                <el-input
                  v-model="phoneFormData.verifyCode"
                  placeholder="请输入验证码"
                  prefix-icon="Message"
                  size="large"
                />
                <el-button
                  type="primary"
                  :disabled="countdown > 0"
                  @click="sendCode"
                  size="large"
                >
                  {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              @click="handlePhoneLogin"
              :loading="isLoading"
            >
              登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="switch-mode">
        <span v-if="isLogin">还没有账号？<a href="#" @click.prevent="switchToRegister">立即注册</a></span>
        <span v-else>已有账号？<a href="#" @click.prevent="switchToLogin">立即登录</a></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Close, Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'

const emit = defineEmits(['close'])
const userStore = useUserStore()

const isLogin = ref(true)
const loginType = ref('password')
const isLoading = ref(false)
const countdown = ref(0)

const usernameExists = ref(false)
const isCheckingUsername = ref(false)
const usernameChecked = ref(false)
const captchaId = ref('')
const captchaImage = ref('')

const loginFormRef = ref()
const registerFormRef = ref()
const phoneFormRef = ref()

// ========== 登录表单数据 ==========
const loginFormData = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

// ========== 注册表单数据 ==========
const registerFormData = reactive({
  username: '',
  password: ''
})

// ========== 手机号表单数据 ==========
const phoneFormData = reactive({
  phone: '',
  verifyCode: ''
})

// ========== 登录验证规则 ==========
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

// ========== 注册验证规则 ==========
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && usernameChecked.value && usernameExists.value) {
          callback(new Error('该用户名已被注册'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度在 6 到 30 个字符', trigger: 'blur' }
  ]
}

// ========== 手机号验证规则 ==========
const phoneRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

// ========== 刷新验证码 ==========
async function refreshCaptcha() {
  try {
    const res = await userApi.generateCaptcha()
    console.log('验证码接口返回:', res)
    if (res.data) {
      captchaId.value = res.data.captchaId
      captchaImage.value = res.data.captchaImage
      console.log('captchaImage 前50字符:', res.data.captchaImage?.substring(0, 50))
    }
  } catch (error) {
    console.error('获取验证码失败:', error)
    ElMessage.error('获取验证码失败')
  }
}

// ========== 加载验证码 ==========
async function loadCaptcha() {
  if (!captchaId.value) {
    await refreshCaptcha()
  }
}

onMounted(() => {
  loadCaptcha()
})

// ========== 切换到注册模式 ==========
function switchToRegister() {
  isLogin.value = false
  
  // 清空注册表单
  registerFormData.username = ''
  registerFormData.password = ''
  
  // 重置用户名校验状态
  resetUsernameCheck()
  
  // 清除验证提示
  if (registerFormRef.value) {
    registerFormRef.value.clearValidate()
  }
}

// ========== 切换到登录模式 ==========
function switchToLogin() {
  isLogin.value = true
  
  loginFormData.username = ''
  loginFormData.password = ''
  loginFormData.captchaCode = ''
  
  captchaId.value = ''
  captchaImage.value = ''
  
  if (loginFormRef.value) {
    loginFormRef.value.clearValidate()
  }
  
  loadCaptcha()
}

// ========== 处理用户名输入 ==========
function handleUsernameInput() {
  if (usernameChecked.value || usernameExists.value) {
    usernameChecked.value = false
    usernameExists.value = false
  }
}

// ========== 重置用户名校验状态 ==========
function resetUsernameCheck() {
  usernameExists.value = false
  isCheckingUsername.value = false
  usernameChecked.value = false
}

// ========== 校验用户名是否重复（仅注册模式）==========
async function checkUsername() {
  if (isLogin.value || !registerFormData.username) return
  
  const username = registerFormData.username.trim()
  
  if (username.length < 3 || username.length > 20) {
    resetUsernameCheck()
    return
  }
  
  isCheckingUsername.value = true
  
  try {
    const result = await userApi.registerPre(username)
    
    if (result.code === 200) {
      usernameExists.value = false
      usernameChecked.value = true
    } else if (result.code === 216) {
      usernameExists.value = true
      usernameChecked.value = true
      ElMessage.error(result.message || '该用户名已被注册')
    } else {
      usernameExists.value = false
      usernameChecked.value = true
    }
  } catch (error) {
    console.error('用户名校验请求失败:', error)
    resetUsernameCheck()
  } finally {
    isCheckingUsername.value = false
  }
}

// ========== 执行登录 ==========
async function handleLogin() {
  if (!loginFormRef.value) return
  
  try {
    await loginFormRef.value.validate()
  } catch (error) {
    return
  }

  isLoading.value = true

  try {
    const res = await userApi.simpleLogin({
      username: loginFormData.username,
      password: loginFormData.password,
      captchaId: captchaId.value,
      captchaCode: loginFormData.captchaCode
    })
    
    if (res.data && res.data.tokenValue) {
      userStore.setToken(res.data.tokenValue)
      
      // 🔑🔑🔑 关键：登录成功后获取完整的用户信息
      const userId = res.data.loginId || loginFormData.username
      let userInfo = {
        username: loginFormData.username,
        userId: userId,
        avatar: ''
      }
      
      try {
        const userRes = await userApi.getUserInfo(userId)
        if (userRes.code === 200 && userRes.data) {
          userInfo = {
            ...userInfo,
            ...userRes.data,
            userId: userId
          }
        }
      } catch (userError) {
        console.warn('[LoginDialog] ⚠️ 获取用户信息失败:', userError)
        // 如果获取用户信息失败，尝试只获取头像
        try {
          const avatarRes = await userApi.getAvatarUrl(userId)
          if (avatarRes.data) {
            userInfo.avatar = avatarRes.data
          }
        } catch (avatarError) {
          console.warn('[LoginDialog] ⚠️ 获取头像失败:', avatarError)
        }
      }
      
      userStore.setUserInfo(userInfo)
      ElMessage.success('登录成功！')
      emit('close')
    }
  } catch (error) {
    console.error('登录失败:', error)
    refreshCaptcha()
    loginFormData.captchaCode = ''
  } finally {
    isLoading.value = false
  }
}

// ========== 执行注册 ==========
async function handleRegister() {
  if (!registerFormRef.value) return
  
  try {
    await registerFormRef.value.validate()
  } catch (error) {
    return
  }

  if (usernameExists.value) {
    ElMessage.error('该用户名已被注册，请更换其他用户名')
    return
  }

  isLoading.value = true

  try {
    const res = await userApi.simpleRegister({
      username: registerFormData.username,
      password: registerFormData.password
    })
    
    ElMessage.success('注册成功！请登录')
    
    // 注册成功后切换到登录模式，清空所有数据
    setTimeout(() => {
      switchToLogin()
    }, 1000)
    
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    isLoading.value = false
  }
}

// ========== 发送手机验证码 ==========
async function sendCode() {
  if (!phoneFormRef.value) return

  try {
    await phoneFormRef.value.validateField('phone')
  } catch (error) {
    return
  }

  try {
    await userApi.loginWithPhoneCodePre(phoneFormData.phone)
    ElMessage.success('验证码已发送！')
    
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

// ========== 手机号登录 ==========
async function handlePhoneLogin() {
  if (!phoneFormRef.value) return

  try {
    await phoneFormRef.value.validate()
  } catch (error) {
    return
  }

  isLoading.value = true

  try {
    const res = await userApi.loginWithPhoneCode({
      phone: phoneFormData.phone,
      verifyCode: phoneFormData.verifyCode
    })

    if (res.data && res.data.tokenValue) {
      userStore.setToken(res.data.tokenValue)
      
      // 🔑🔑🔑 关键：登录成功后获取完整的用户信息
      const userId = res.data.loginId || phoneFormData.phone
      let userInfo = {
        phone: phoneFormData.phone,
        userId: userId,
        avatar: ''
      }
      
      try {
        const userRes = await userApi.getUserInfo(userId)
        if (userRes.code === 200 && userRes.data) {
          userInfo = {
            ...userInfo,
            ...userRes.data,
            userId: userId
          }
        }
      } catch (userError) {
        console.warn('[LoginDialog] ⚠️ 获取用户信息失败:', userError)
        // 如果获取用户信息失败，尝试只获取头像
        try {
          const avatarRes = await userApi.getAvatarUrl(userId)
          if (avatarRes.data) {
            userInfo.avatar = avatarRes.data
          }
        } catch (avatarError) {
          console.warn('[LoginDialog] ⚠️ 获取头像失败:', avatarError)
        }
      }
      
      userStore.setUserInfo(userInfo)
      ElMessage.success('登录成功！')
      emit('close')
    }
  } catch (error) {
    console.error('手机号登录失败:', error)
  } finally {
    isLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-overlay {
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

.login-dialog {
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

.login-tabs {
  margin-bottom: 24px;
}

.code-input-wrapper {
  display: flex;
  gap: 12px;

  .el-input {
    flex: 1;
  }

  .el-button {
    width: 120px;
    flex-shrink: 0;
  }
}

.captcha-input-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;

  .el-input {
    flex: 1;
  }

  .captcha-image {
    width: 120px;
    height: 40px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    flex-shrink: 0;
    background-color: #f5f7fa;
    transition: border-color 0.2s;

    &:hover {
      border-color: #409eff;
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .captcha-placeholder {
      font-size: 12px;
      color: #909399;
    }
  }
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  margin-top: 8px;
}

.switch-mode {
  text-align: center;
  font-size: 14px;
  color: #666;
  
  a {
    color: #1890ff;
    text-decoration: none;
    
    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
