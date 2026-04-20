export class ApiBusinessError<T = unknown> extends Error {
  readonly code: number
  readonly payload?: T

  constructor(code: number, message: string, payload?: T) {
    super(message)
    this.name = 'ApiBusinessError'
    this.code = code
    this.payload = payload
  }
}

export class ApiHttpError extends Error {
  readonly status?: number

  constructor(message: string, status?: number) {
    super(message)
    this.name = 'ApiHttpError'
    this.status = status
  }
}
