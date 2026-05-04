import request from '@/utils/request'

export const userProfileApi = {
  getUserProfile() {
    return request.get('/profile')
  },

  /**
   * 全量覆盖指定维度的画像条目
   * @param {string} dimension 维度名称，如 "技术偏好"
   * @param {string[]} values 该维度的所有值，如 ["Java", "Go"]，传空数组则删除该维度
   */
  updateUserProfile(dimension, values) {
    return request.put('/profile', { dimension, values })
  },

  /**
   * 删除画像维度或单个条目
   * @param {string} dimension 维度名称
   * @param {string} [value] 可选，指定要删除的条目值；不传则删除整个维度
   */
  deleteUserProfile(dimension, value) {
    const params = { dimension }
    if (value) params.value = value
    return request.delete('/profile', { params })
  }
}
