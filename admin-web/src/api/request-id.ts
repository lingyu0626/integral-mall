export function createRequestId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  const seed = `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`
  return `rid-${seed}`
}

export function shouldAttachRequestId(method?: string): boolean {
  if (!method) return false
  const normalized = method.toUpperCase()
  return normalized === 'POST' || normalized === 'PUT' || normalized === 'DELETE'
}
