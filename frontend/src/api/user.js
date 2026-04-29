import request from '@/utils/request'

export const userApi = {
  // 获取登录验证码图片
  // 后端：@GetMapping("/captcha/generate")
  generateCaptcha() {
    return request.get('/captcha/generate')
  },

  // 用户登录-统一入口（策略模式）
  // 后端：@PostMapping("/login") + @RequestParam
  // loginType: password / phone
  login(data) {
    return request.post('/user/login', null, { params: data })
  },

  // 发送验证码（手机号登录前置）
  // 后端：@RequestMapping("/LoginWithPhoneCodePre") + @RequestParam("phone")
  loginWithPhoneCodePre(phone) {
    return request.post('/user/LoginWithPhoneCodePre', null, { params: { phone } })
  },

  // 用户注册-统一入口（策略模式）
  // 后端：@PostMapping("/register") + @RequestParam
  // registerType: password / phone
  register(data) {
    return request.post('/user/register', null, { params: data })
  },

  // 注册前动态校验用户名是否已经存在
  // 后端：@RequestMapping("/registerPre") + @RequestParam("username")
  registerPre(username) {
    return request.post('/user/registerPre', null, {
      params: { username },
      customHandleBusinessError: true
    })
  },

  // 🔑 关键修正：用户退出登录 - 改为GET方法（符合controller.md文档）
  // 后端：@GetMapping("/logout")
  logout() {
    return request.get('/user/logout')
  },

  // minio图片头像文件上传
  // 后端：@PostMapping("/fileUpload") + MultipartFile
  fileUpload(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/user/fileUpload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 🔑 关键修正：minio图片头像文件查询 - 改为getAvatarUrl（符合controller.md文档）
  // 后端：@GetMapping("/getAvatarUrl") + @RequestParam("userId")
  getAvatarUrl(userId) {
    return request.get('/user/getAvatarUrl', { params: { userId } })
  },

  // 获取用户基本信息
  // 后端：@GetMapping + @RequestParam("userId")
  getUserInfo(userId) {
    return request.get('/user', { params: { userId } })
  },

  // 更新用户基本信息
  // 后端：@PutMapping + @RequestBody UserVo
  updateUserInfo(userVo) {
    return request.put('/user', userVo)
  }
}
