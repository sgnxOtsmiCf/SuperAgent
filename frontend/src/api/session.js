import request from '@/utils/request'

export const sessionApi = {
 // 查询当前用户的所有会话
 // 后端：@GetMapping + @RequestParam("agentId") 必须参数
  getAllSessions(agentId) {
    return request.get('/session', { params: { agentId } })
  },

 // 查询当前用户的分页会话
 // 后端：@GetMapping("/page") + @RequestParam agentId必须
  getSessionsPage(agentId, pageNo = 1, pageSize = 10) {
    return request.get('/session/page', {
      params: { agentId, pageNo, pageSize }
    })
  },

 // 根据会话id查询会话记录
 // 后端：@GetMapping("/single") + @RequestParam agentId必须
  getSessionBySessionId(agentId, sessionId) {
    return request.get('/session/single', {
      params: { agentId, sessionId }
    })
  },

 // 删除会话
 // 后端：@DeleteMapping + @RequestParam agentId必须
  deleteSessionById(agentId, sessionId) {
    return request.delete('/session', {
      params: { agentId, sessionId }
    })
  },

 // 修改会话标题名称
 // 后端：@PostMapping + @RequestParam("agentId") + @RequestParam("sessionId") + @RequestParam("sessionName")
  updateSessionTitle(agentId, sessionId, sessionName) {
    return request.post('/session', null, {
      params: { agentId, sessionId, sessionName }
    })
  },

 // ==================== 归档相关接口 ====================

 // 归档session会话(异步归档到MySQL)
 // 后端：@PostMapping("/archive") + @RequestParam("agentId") + @RequestParam("sessionId")
  archiveSession(agentId, sessionId) {
    return request.post('/session/archive', null, {
      params: { agentId, sessionId }
    })
  },

 // 查询会话归档状态
 // 后端：@GetMapping("/archive/status") + @RequestParam("sessionId")
  getArchiveStatus(sessionId) {
    return request.get('/session/archive/status', {
      params: { sessionId }
    })
  },

 // 分页查询归档数据
 // 后端：@GetMapping("/archive") + @RequestParam("agentId") + @RequestParam("pageNo") + @RequestParam("pageSize")
  getArchiveSessions(agentId, pageNo = 1, pageSize = 10) {
    return request.get('/session/archive', {
      params: { agentId, pageNo, pageSize }
    })
  },

 // ==================== 置顶相关接口 ====================

 // 置顶会话
 // 后端：@GetMapping("/isTop") + @RequestParam("agentId") + @RequestParam("sessionId")
  setTopSession(agentId, sessionId) {
    return request.get('/session/isTop', {
      params: { agentId, sessionId }
    })
  },

 // 取消置顶会话
 // 后端：@GetMapping("/isUpTop") + @RequestParam("agentId") + @RequestParam("sessionId")
  setUpTopSession(agentId, sessionId) {
    return request.get('/session/isUpTop', {
      params: { agentId, sessionId }
    })
  }
}
