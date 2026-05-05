/**
 * 生成唯一的聊天会话 ID
 * @param {string} prefix - ID 前缀
 * @returns {string} 格式为 prefix-timestamp-random 的唯一 ID
 */
export function createChatId(prefix = 'chat') {
  const timestamp = Date.now()
  const random = Math.random().toString(36).substring(2, 9)
  return `${prefix}-${timestamp}-${random}`
}

/**
 * 从 localStorage 获取会话数据
 * @param {string} key - 存储键名
 * @returns {any} 解析后的数据，如果不存在或解析失败则返回 null
 */
export function getSessionData(key) {
  try {
    const data = localStorage.getItem(key)
    return data ? JSON.parse(data) : null
  } catch (error) {
    console.error('Failed to get session data:', error)
    return null
  }
}

/**
 * 保存会话数据到 localStorage
 * @param {string} key - 存储键名
 * @param {any} value - 要保存的数据
 */
export function setSessionData(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch (error) {
    console.error('Failed to set session data:', error)
  }
}

/**
 * 删除会话数据
 * @param {string} key - 存储键名
 */
export function removeSessionData(key) {
  try {
    localStorage.removeItem(key)
  } catch (error) {
    console.error('Failed to remove session data:', error)
  }
}

/**
 * 清空所有会话数据
 */
export function clearAllSessionData() {
  try {
    localStorage.clear()
  } catch (error) {
    console.error('Failed to clear session data:', error)
  }
}
