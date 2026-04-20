import axios, { AxiosHeaders, type AxiosError, type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { clearToken, getToken } from './auth-storage'
import { ApiBusinessError, ApiHttpError } from './errors'
import { createRequestId, shouldAttachRequestId } from './request-id'
import type { ApiResponse, RequestScope } from './types'

export type RequestConfig<D = unknown> = AxiosRequestConfig<D> & {
  scope?: RequestScope
  skipAuth?: boolean
  withRequestId?: boolean
}

const AUTH_ERROR_CODES = new Set([4010, 4011, 4012])

function resetAuthState() {
  clearToken('admin')
  clearToken('app')
}

function setHeader(config: InternalAxiosRequestConfig, name: string, value: string) {
  const headers = AxiosHeaders.from(config.headers)
  headers.set(name, value)
  config.headers = headers
}

function resolveBaseURL() {
  return import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
}

function trimTailSlash(url: string) {
  return url.replace(/\/+$/, '')
}

function resolveOrigin(url: string) {
  const match = trimTailSlash(url).match(/^(https?:\/\/[^/]+)/i)
  return match?.[1] ?? ''
}

function normalizeImageLikeUrl(value: unknown): unknown {
  if (typeof value !== 'string' || !value) return value
  const baseURL = trimTailSlash(resolveBaseURL())
  const baseOrigin = resolveOrigin(baseURL)

  if (value.startsWith('/api/')) {
    return baseURL ? `${baseURL}${value}` : value
  }

  const localMatch = value.match(/^https?:\/\/(?:localhost|127\.0\.0\.1)(?::\d+)?(\/api\/.*)$/i)
  if (localMatch?.[1]) {
    if (baseOrigin) return `${baseOrigin}${localMatch[1]}`
    if (baseURL) return `${baseURL}${localMatch[1]}`
  }
  return value
}

function normalizePayloadUrls(payload: unknown): unknown {
  if (Array.isArray(payload)) {
    return payload.map(normalizePayloadUrls)
  }
  if (payload && typeof payload === 'object') {
    const next: Record<string, unknown> = {}
    Object.entries(payload as Record<string, unknown>).forEach(([key, value]) => {
      next[key] = normalizePayloadUrls(value)
    })
    return next
  }
  return normalizeImageLikeUrl(payload)
}

function createHttpClient(): AxiosInstance {
  const client = axios.create({
    baseURL: resolveBaseURL(),
    timeout: 15000,
  })

  client.interceptors.request.use((config) => {
    const scope = (config as RequestConfig).scope ?? 'admin'
    const skipAuth = Boolean((config as RequestConfig).skipAuth)
    const withRequestId = (config as RequestConfig).withRequestId

    setHeader(config, 'Accept', 'application/json')

    if (!skipAuth) {
      const token = getToken(scope)
      if (token) {
        setHeader(config, 'Authorization', `Bearer ${token}`)
      }
    }

    const shouldSetRequestId = withRequestId ?? shouldAttachRequestId(config.method)
    if (shouldSetRequestId) {
      setHeader(config, 'X-Request-Id', createRequestId())
    }

    return config
  })

  client.interceptors.response.use(
    (response) => {
      const payload = response.data as ApiResponse<unknown>
      const hasUnifiedShape =
        payload &&
        typeof payload === 'object' &&
        typeof payload.code === 'number' &&
        typeof payload.message === 'string' &&
        'data' in payload

      if (!hasUnifiedShape) {
        return response.data
      }

      if (payload.code === 0) {
        return normalizePayloadUrls(payload.data)
      }

      if (AUTH_ERROR_CODES.has(payload.code)) {
        resetAuthState()
        if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }

      throw new ApiBusinessError(payload.code, payload.message, payload.data)
    },
    (error: AxiosError<{ message?: string }>) => {
      const status = error.response?.status
      const message = error.response?.data?.message || error.message || '网络异常'

      if (status === 401) {
        resetAuthState()
      }

      return Promise.reject(new ApiHttpError(message, status))
    },
  )

  return client
}

export const http = createHttpClient()

export async function request<T = unknown, D = unknown>(config: RequestConfig<D>): Promise<T> {
  const result = await http.request<T, T, D>(config)
  return result
}

export function get<T = unknown>(url: string, config?: RequestConfig): Promise<T> {
  return request<T>({ ...config, method: 'GET', url })
}

export function post<T = unknown, D = unknown>(url: string, data?: D, config?: RequestConfig<D>): Promise<T> {
  return request<T, D>({ ...config, method: 'POST', url, data })
}

export function put<T = unknown, D = unknown>(url: string, data?: D, config?: RequestConfig<D>): Promise<T> {
  return request<T, D>({ ...config, method: 'PUT', url, data })
}

export function del<T = unknown>(url: string, config?: RequestConfig): Promise<T> {
  return request<T>({ ...config, method: 'DELETE', url })
}
