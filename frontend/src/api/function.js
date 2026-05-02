import request from '@/utils/request'

export const functionApi = {
 // 获取工具列表
 // 后端：@GetMapping("/tool") + @SaCheckLogin
  getTools() {
    return request.get('/function/tool')
  },

 // 获取 skills 列表
 // 后端：@GetMapping("/skills") + @SaCheckLogin
  getSkills() {
    return request.get('/function/skills')
  }
}
