function createRequestId() {
  const timestamp = Date.now().toString(36)
  const random = Math.random().toString(36).slice(2, 12)
  return `rid-${timestamp}-${random}`
}

function shouldAttachRequestId(method) {
  return ['POST', 'PUT', 'DELETE'].includes(String(method || '').toUpperCase())
}

module.exports = {
  createRequestId,
  shouldAttachRequestId,
}
