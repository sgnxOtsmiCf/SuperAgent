import request from '@/utils/request'

export const versionApi = {
  /**
   * 获取版本基本信息
   */
  getVersion() {
    return request({
      url: '/version',
      method: 'get'
    })
  },

  /**
   * 获取版本详细信息（包含优点和不足）
   */
  getVersionDetail() {
    return request({
      url: '/version/all',
      method: 'get'
    })
  }
}
