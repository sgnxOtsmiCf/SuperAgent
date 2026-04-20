import request from '@/utils/request'

export const messageApi = {
  /**
   * 根据消息id查询单条数据
   * @param {string} sessionId - 会话ID
   * @param {number} messageId - 消息ID
   */
  getMessageByMessageId(sessionId, messageId) {
    return request.get('/message/single', {
      params: { sessionId, messageId }
    })
  },

  /**
   * 根据消息id删除单条数据（仅归档消息支持）
   * @param {string} sessionId - 会话ID
   * @param {number} messageId - 消息ID
   */
  deleteMessageByMessageId(sessionId, messageId) {
    return request.delete('/message/single', {
      params: { sessionId, messageId }
    })
  },

  /**
   * 根据消息列表ids批量删除（仅归档消息支持）
   * @param {string} sessionId - 会话ID
   * @param {number[]} messageIds - 消息ID数组
   */
  deleteBatchMessageByMessageId(sessionId, messageIds) {
    return request.delete('/message', {
      params: { sessionId, messageIds }
    })
  }
}
