/**
 * 错误代码处理工具
 * 根据后端 ResultCodeEnum 定义，区分系统错误和权限错误
 */

// 系统错误代码列表（需要提示"稍后回复，请重试"的错误）
const SYSTEM_ERROR_CODES = [
  201,  // FAIL - 失败
  202,  // SERVICE_ERROR - 服务异常
  204,  // DATA_ERROR - 数据异常
  205,  // ILLEGAL_REQUEST - 非法请求
  206,  // REPEAT_SUBMIT - 重复提交
  210,  // ARGUMENT_VALID_ERROR - 参数校验异常
  1001, // AGENT_FAIL - agent执行异常
  1002, // MODEL_FAIL - 模型网络连接不稳定
]

// 权限相关错误代码（不需要提示"稍后重试"的错误）
const AUTH_ERROR_CODES = [
  208,  // LOGIN_AUTH - 未登陆
  209,  // PERMISSION - 没有权限
  214,  // ACCOUNT_ERROR - 用户名不正确
  215,  // PASSWORD_ERROR - 密码不正确
  216,  // ACCOUNT_REPEAT - 用户名重复
  217,  // ACCOUNT_STOP - 账号已停用
  218,  // PHONE_FALSE - 手机号不合法
  219,  // GET_USERID_FAIL - 获取用户id失败
  300,  // SIGN_ERROR - 签名错误
  301,  // SIGN_OVERDUE - 签名已过期
]

// 会话相关错误代码
const SESSION_ERROR_CODES = [
  220,  // SESSION_ID_NO_EXITS - 当前用户的此agent不存在此会话id或会话id不合法
  221,  // AGENT_SESSION_EMPTY / AGENT_ID_NO_EXIST - sessionId为空或agentId不存在
  222,  // SESSION_ID_EXPIRED - sessionId 已经过期，会话自动销毁
  223,  // SESSION_NO_FIND - 会话没有发现，或已经删除
  224,  // SESSION_ARCHIVE - 会话已归档
  225,  // SESSION_DELETE - 会话删除失败
]

// 消息相关错误代码
const MESSAGE_ERROR_CODES = [
  250,  // MESSAGE_NO_FIND - 消息不存在，或没有归档
  251,  // MESSAGE_DELETE_FAIL - 消息删除失败
]

// 文件相关错误代码
const FILE_ERROR_CODES = [
  501,  // FILE_UPLOAD_FAIL - minio用户图像上传失败
  502,  // FILE_GET_URL_FAIL - minio获取地址失败
  503,  // FILE_DOWNLOAD_FAIL - minio获取地址失败
  504,  // FILE_DELETE_FAIL - minio删除文件失败
  505,  // BUCKET_SELECT_FAIL - minio查询桶失败
  506,  // BUCKET_CREATE_FAIL - minio创建失败
]

// 验证码相关错误代码
const CAPTCHA_ERROR_CODES = [
  401,  // CAPTCHA_EMPTY - 验证码不能为空
  402,  // CAPTCHA_EXPIRED - 验证码已过期
  403,  // CAPTCHA_ERROR - 验证码错误
  404,  // CAPTCHA_GENERATE_FAIL - 验证码生成失败
]

/**
 * 判断是否为系统错误（需要提示"稍后重试"）
 * @param {number|string} code - 错误代码
 * @returns {boolean}
 */
export function isSystemError(code) {
  if (!code) return false
  const codeNum = typeof code === 'string' ? parseInt(code, 10) : code
  return SYSTEM_ERROR_CODES.includes(codeNum)
}

/**
 * 判断是否为权限错误
 * @param {number|string} code - 错误代码
 * @returns {boolean}
 */
export function isAuthError(code) {
  if (!code) return false
  const codeNum = typeof code === 'string' ? parseInt(code, 10) : code
  return AUTH_ERROR_CODES.includes(codeNum)
}

/**
 * 判断是否为会话错误
 * @param {number|string} code - 错误代码
 * @returns {boolean}
 */
export function isSessionError(code) {
  if (!code) return false
  const codeNum = typeof code === 'string' ? parseInt(code, 10) : code
  return SESSION_ERROR_CODES.includes(codeNum)
}

/**
 * 获取友好的错误提示消息
 * @param {number|string} code - 错误代码
 * @param {string} originalMessage - 原始错误消息
 * @returns {string} 处理后的错误消息
 */
export function getFriendlyErrorMessage(code, originalMessage) {
  if (!code) return originalMessage || '操作失败，请稍后重试'
  
  const codeNum = typeof code === 'string' ? parseInt(code, 10) : code
  
 // 系统错误 - 添加"稍后重试"提示
  if (SYSTEM_ERROR_CODES.includes(codeNum)) {
    const baseMessage = originalMessage || '服务暂时不可用'
 // 如果原始消息已经包含"稍后"或"重试"，则不再添加
    if (baseMessage.includes('稍后') || baseMessage.includes('重试')) {
      return baseMessage
    }
    return `${baseMessage}，请稍后重试`
  }
  
 // 权限错误 - 直接返回原始消息
  if (AUTH_ERROR_CODES.includes(codeNum)) {
    return originalMessage || '权限验证失败'
  }
  
 // 会话错误 - 添加"稍后重试"提示
  if (SESSION_ERROR_CODES.includes(codeNum)) {
    const baseMessage = originalMessage || '会话操作失败'
    if (baseMessage.includes('稍后') || baseMessage.includes('重试')) {
      return baseMessage
    }
    return `${baseMessage}，请稍后重试`
  }
  
 // 消息错误 - 添加"稍后重试"提示
  if (MESSAGE_ERROR_CODES.includes(codeNum)) {
    const baseMessage = originalMessage || '消息操作失败'
    if (baseMessage.includes('稍后') || baseMessage.includes('重试')) {
      return baseMessage
    }
    return `${baseMessage}，请稍后重试`
  }
  
 // 文件错误 - 添加"稍后重试"提示
  if (FILE_ERROR_CODES.includes(codeNum)) {
    const baseMessage = originalMessage || '文件操作失败'
    if (baseMessage.includes('稍后') || baseMessage.includes('重试')) {
      return baseMessage
    }
    return `${baseMessage}，请稍后重试`
  }
  
 // 验证码错误 - 直接返回原始消息
  if (CAPTCHA_ERROR_CODES.includes(codeNum)) {
    return originalMessage || '验证码错误'
  }
  
 // 默认处理
  return originalMessage || '操作失败，请稍后重试'
}

/**
 * 解析错误响应
 * @param {Object} error - 错误对象
 * @returns {Object} { code, message, isSystemError, isAuthError }
 */
export function parseError(error) {
  let code = null
  let message = ''
  
 // 尝试从错误对象中提取代码和消息
  if (error?.response?.data) {
    code = error.response.data.code
    message = error.response.data.message || error.response.data.msg
  } else if (error?.code) {
    code = error.code
    message = error.message
  } else if (typeof error === 'string') {
 // 尝试解析JSON错误
    try {
      const parsed = JSON.parse(error)
      code = parsed.code
      message = parsed.message || parsed.msg
    } catch {
      message = error
    }
  }
  
  return {
    code,
    message: getFriendlyErrorMessage(code, message),
    isSystemError: isSystemError(code),
    isAuthError: isAuthError(code),
    isSessionError: isSessionError(code)
  }
}
