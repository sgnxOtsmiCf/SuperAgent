import request from '@/utils/request'

export const userProfileApi = {
  // 获取用户画像
  // 后端：@GetMapping("/{userId}")
  getUserProfile(userId) {
    return request.get(`/profile/${userId}`)
  }
}
