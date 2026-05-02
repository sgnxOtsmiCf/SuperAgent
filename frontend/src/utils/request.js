import axios from 'axios'
import { logger } from '@/utils/logger'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { isSystemError, isSessionError, isAuthError, getFriendlyErrorMessage } from './errorHandler'

// 后端 ResultCodeEnum 错误码映射
const ERROR_CODE_MAP = {
  200: { type: 'success', message: '成功' },
  201: { type: 'error', message: '操作失败' },
  202: { type: 'error', message: '服务异常' },
  204: { type: 'error', message: '数据异常' },
  205: { type: 'warning', message: '非法请求' },
  206: { type: 'warning', message: '重复提交' },
  210: { type: 'warning', message: '参数校验异常' },
  300: { type: 'error', message: '签名错误' },
  301: { type: 'error', message: '签名已过期', needLogout: true },
  208: { type: 'warning', message: '未登录，请先登录', needLogout: true },
  209: { type: 'error', message: '没有权限访问' },
  214: { type: 'warning', message: '用户名不正确' },
  215: { type: 'warning', message: '密码/验证码不正确' },
  216: { type: 'warning', message: '用户名已存在' },
  217: { type: 'error', message: '账号已停用' },
  218: { type: 'warning', message: '手机号不合法' },
  219: { type: 'error', message: '获取用户信息失败', needLogout: true },
  220: { type: 'warning', message: '会话不存在' },
  221: { type: 'error', message: 'Agent执行异常' },
  222: { type: 'warning', message: '会话已过期' },
  223: { type: 'warning', message: '会话未找到' },
  224: { type: 'warning', message: '会话已归档' },
  225: { type: 'error', message: '会话删除失败' },
  250: { type: 'warning', message: '消息不存在' },
  251: { type: 'error', message: '消息删除失败' },
  501: { type: 'error', message: '文件上传失败' },
  502: { type: 'error', message: '获取文件地址失败' },
  503: { type: 'error', message: '文件下载失败' },
  504: { type: 'error', message: '文件删除失败' },
  505: { type: 'error', message: '存储桶查询失败' },
  506: { type: 'error', message: '存储桶创建失败' },
  1001: { type: 'error', message: 'Agent执行异常' },
  1002: { type: 'error', message: '模型网络连接不稳定' }
}

// 新增：需要登出的错误码列表（sa-token 登录过期相关）
const LOGOUT_ERROR_CODES = [208, 301, 219]

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 新增：处理登录过期，自动登出
function handleTokenExpired(message, code) {
  const userStore = useUserStore()

 // 关键修复：无论是否已登录，都要显示提示
 // 如果是208（未登录）且用户已登录，说明登录状态过期，执行登出
 // 如果是208（未登录）且用户未登录，提示用户先登录
  if (code === 208 && !userStore.isLoggedIn) {
 // 用户未登录，提示登录
    ElMessageBox.alert(
      message || '请先登录',
      '未登录',
      {
        confirmButtonText: '去登录',
        type: 'warning',
        callback: () => {
 // 触发自定义事件打开登录对话框
          window.dispatchEvent(new CustomEvent('show-login-dialog'))
        }
      }
    )
  } else {
 // 用户已登录但token过期，执行登出
    ElMessageBox.alert(
      message || '登录已过期，请重新登录',
      '登录过期',
      {
        confirmButtonText: '确定',
        type: 'warning',
        callback: () => {
 // 执行登出
          userStore.logout()
 // 刷新页面或跳转到登录页（根据你的路由配置）
          window.location.reload()
        }
      }
    )
  }
}

// 核心升级：全局错误处理 - 全部使用顶部弹窗通知（ElMessage）
request.interceptors.response.use(
  response => {
    const res = response.data
    const customHandleBusinessError = response.config?.customHandleBusinessError === true

 // 关键：检查后端返回的业务状态码
    if (res.code !== undefined && res.code !== null) {
      if (res.code === 200) {
 // 成功，直接返回数据
        return res
      } else if (customHandleBusinessError) {
        return res
      } else {
 // 关键：检查是否需要自动登出（token过期）
        if (LOGOUT_ERROR_CODES.includes(res.code)) {
          const errorConfig = ERROR_CODE_MAP[res.code] || { type: 'warning', message: '登录已过期' }
          handleTokenExpired(res.message || errorConfig.message, res.code)
          return Promise.reject({
            code: res.code,
            message: res.message || errorConfig.message,
            data: res.data,
            needLogout: true
          })
        }

 // 业务错误：使用顶部弹窗通知（ElMessage）
        const errorConfig = ERROR_CODE_MAP[res.code] || { type: 'error', message: '未知错误' }
        let errorMessage = res.message || errorConfig.message
        
 // 使用错误处理工具获取友好提示（系统错误和会话错误添加"稍后重试"）
        errorMessage = getFriendlyErrorMessage(res.code, errorMessage)

        logger.error(`[API] 业务错误 [${res.code}]:`, errorMessage)

 // 关键：使用 ElMessage 替代 ElMessageBox（自动消失，无需确认）
 // 权限错误使用 warning，系统错误使用 error
        if (isAuthError(res.code) || errorConfig.type === 'warning') {
          ElMessage.warning({
            message: errorMessage,
            duration: 4000,  // 警告显示4秒
            showClose: true,
            grouping: true
          })
        } else {
          ElMessage.error({
            message: errorMessage,
            duration: 5000,  // 错误显示5秒
            showClose: true,
            grouping: true
          })
        }

 // 返回拒绝的Promise，让调用方可以catch处理
        return Promise.reject({
          code: res.code,
          message: errorMessage,
          data: res.data
        })
      }
    }

 // 如果没有code字段，直接返回原始响应
    return res
  },
  error => {
    logger.error('请求错误:', error)

    if (error.response) {
      const status = error.response.status
      const responseData = error.response.data

 // 如果后端返回了带code的错误响应
      if (responseData && responseData.code && responseData.code !== 200) {
        const errorMessage = responseData.message || `请求失败 (${status})`

 // 使用顶部弹窗通知
        ElMessage.error({
          message: errorMessage,
          duration: 5000,
          showClose: true,
          grouping: true
        })

        return Promise.reject({
          code: responseData.code,
          message: errorMessage,
          data: responseData.data
        })
      }

 // HTTP状态码错误处理
      let message = `请求失败 (${status})`
      if (status === 401) {
        message = '登录已过期，请重新登录'
 // 关键：401 状态码也触发自动登出
        handleTokenExpired(message)
        return Promise.reject({
          code: status,
          message: message,
          needLogout: true
        })
      } else if (status === 403) {
        message = '没有权限访问'
      } else if (status === 404) {
        message = '请求的资源不存在'
      } else if (status === 500) {
        message = '服务器内部错误，请稍后重试'
      } else if (status === 502 || status === 503 || status === 504) {
        message = '服务暂时不可用，请稍后重试'
      }

 // 使用顶部弹窗通知（非确认对话框）
      ElMessage.error({
        message: message,
        duration: 5000,
        showClose: true,
        grouping: true
      })

      return Promise.reject({
        code: status,
        message: message,
        data: responseData
      })
    } else if (error.message.includes('timeout')) {
 // 超时错误
      ElMessage.warning({
        message: '请求超时，请稍后重试',
        duration: 4000,
        showClose: true,
        grouping: true
      })

      return Promise.reject({ code: -1, message: '请求超时' })
    } else if (error.message.includes('ERR_ABORTED') || error.name === 'AbortError') {
 // 主动取消/中断（静默处理，不提示）
      return Promise.reject({ code: -3, message: '请求已中断' })
    } else {
 // 其他网络错误
      ElMessage.error({
        message: '网络连接失败，请检查后端服务是否启动',
        duration: 6000,  // 网络错误显示更久
        showClose: true,
        grouping: true
      })

      return Promise.reject({ code: -2, message: '网络连接失败' })
    }
  }
)

export default request
