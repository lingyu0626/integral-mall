class ApiBusinessError extends Error {
  constructor(code, message, payload) {
    super(message || '业务处理失败')
    this.name = 'ApiBusinessError'
    this.code = code
    this.payload = payload
  }
}

function assertApiSuccess(response) {
  if (!response || typeof response !== 'object') {
    throw new ApiBusinessError(-1, '响应结构非法', response)
  }

  if (typeof response.code !== 'number') {
    throw new ApiBusinessError(-1, '响应缺少 code', response)
  }

  if (response.code !== 0) {
    throw new ApiBusinessError(response.code, response.message || '业务失败', response.data)
  }

  return response.data
}

module.exports = {
  ApiBusinessError,
  assertApiSuccess,
}
