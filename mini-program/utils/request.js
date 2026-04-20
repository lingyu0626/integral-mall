const { API_BASE_URL } = require('./env')
const { getToken, clearToken } = require('./auth')
const { createRequestId, shouldAttachRequestId } = require('./request-id')
const { assertApiSuccess } = require('./api-response')

const DEFAULT_IMAGE_WIDTH = 480
const DEFAULT_IMAGE_QUALITY = 72
const MIN_IMAGE_WIDTH = 160
const MAX_IMAGE_WIDTH = 1080

let cachedDevicePixelWidth = 0

function trimTailSlash(url = '') {
  return String(url).replace(/\/+$/, '')
}

function safeOrigin(url = '') {
  const normalized = trimTailSlash(url)
  const match = normalized.match(/^(https?:\/\/[^/]+)/i)
  return match ? match[1] : normalized
}

function resolveDevicePixelWidth() {
  if (cachedDevicePixelWidth > 0) return cachedDevicePixelWidth
  try {
    if (typeof wx !== 'undefined') {
      const info =
        (typeof wx.getWindowInfo === 'function' ? wx.getWindowInfo() : null) ||
        (typeof wx.getSystemInfoSync === 'function' ? wx.getSystemInfoSync() : null) ||
        {}
      const logicalWidth = Number(info.windowWidth || info.screenWidth || 375) || 375
      const dpr = Math.min(2, Math.max(1, Number(info.pixelRatio || 1) || 1))
      cachedDevicePixelWidth = Math.max(360, Math.min(MAX_IMAGE_WIDTH, Math.round(logicalWidth * dpr)))
    } else {
      cachedDevicePixelWidth = DEFAULT_IMAGE_WIDTH
    }
  } catch (_) {
    cachedDevicePixelWidth = DEFAULT_IMAGE_WIDTH
  }
  return cachedDevicePixelWidth
}

function pickResizeProfile(path = []) {
  const text = Array.isArray(path) ? path.join('.').toLowerCase() : ''
  const deviceWidth = resolveDevicePixelWidth()
  const maxWidth = Math.max(DEFAULT_IMAGE_WIDTH, Math.min(MAX_IMAGE_WIDTH, Math.round(deviceWidth * 1.1)))

  let width = DEFAULT_IMAGE_WIDTH
  let quality = DEFAULT_IMAGE_QUALITY

  if (/avatar|icon|logo|thumb|qrcode|qr_code|qr_url|menu/.test(text)) {
    width = 220
    quality = 70
  } else if (/banner|carousel|slide|hero/.test(text)) {
    width = 720
    quality = 74
  } else if (/detail/.test(text)) {
    width = 860
    quality = 76
  } else if (/main_image|cover|snapshot|image_url/.test(text)) {
    width = 520
    quality = 72
  }

  return {
    width: Math.max(MIN_IMAGE_WIDTH, Math.min(maxWidth, width)),
    quality: Math.max(55, Math.min(85, quality)),
  }
}

function upsertNumericQuery(url, key, targetValue) {
  if (!url || !key) return url

  const hashIndex = url.indexOf('#')
  const hashPart = hashIndex >= 0 ? url.slice(hashIndex) : ''
  const beforeHash = hashIndex >= 0 ? url.slice(0, hashIndex) : url
  const queryIndex = beforeHash.indexOf('?')
  const base = queryIndex >= 0 ? beforeHash.slice(0, queryIndex) : beforeHash
  const queryString = queryIndex >= 0 ? beforeHash.slice(queryIndex + 1) : ''
  const queryList = queryString ? queryString.split('&').filter(Boolean) : []

  let found = false
  const keyLower = String(key).toLowerCase()
  const safeDecode = (text) => {
    try {
      return decodeURIComponent(text || '')
    } catch (_) {
      return String(text || '')
    }
  }
  const nextQueryList = queryList.map((entry) => {
    const equalIndex = entry.indexOf('=')
    const rawKey = equalIndex >= 0 ? entry.slice(0, equalIndex) : entry
    const rawValue = equalIndex >= 0 ? entry.slice(equalIndex + 1) : ''
    if (safeDecode(rawKey).toLowerCase() !== keyLower) return entry

    found = true
    const current = Number(rawValue)
    const nextValue =
      Number.isFinite(current) && current > 0
        ? Math.min(current, targetValue)
        : targetValue
    return `${encodeURIComponent(key)}=${nextValue}`
  })

  if (!found) {
    nextQueryList.push(`${encodeURIComponent(key)}=${targetValue}`)
  }

  const nextQuery = nextQueryList.join('&')
  return `${base}${nextQuery ? `?${nextQuery}` : ''}${hashPart}`
}

function appendResizeParamIfNeeded(url, path = []) {
  if (typeof url !== 'string' || !url) return url
  if (!/\/api\/v1\/admin\/files\/\d+\/content/i.test(url)) return url
  const profile = pickResizeProfile(path)
  // 优先压缩到移动端展示所需尺寸，减少下载体积
  let next = upsertNumericQuery(url, 'w', profile.width)
  // JPEG 可进一步降低体积，后端会在支持时应用 q 参数
  next = upsertNumericQuery(next, 'q', profile.quality)
  return next
}

function optimizeKnownImageCdn(url, path = []) {
  if (typeof url !== 'string' || !url) return url
  const profile = pickResizeProfile(path)
  const pathText = Array.isArray(path) ? path.join('.').toLowerCase() : ''
  const shouldForceSquare =
    !!pathText &&
    !/banner|carousel|slide|hero/.test(pathText) &&
    /(main_image|cover|snapshot|thumb)/.test(pathText)
  const picsumMatch = url.match(/^(https?:\/\/picsum\.photos\/(?:id\/\d+\/)?)(\d+)\/(\d+)(\?.*)?$/i)
  if (!picsumMatch) return url

  const originalWidth = Number(picsumMatch[2] || 0)
  const originalHeight = Number(picsumMatch[3] || 0)
  if (!originalWidth || !originalHeight) return url

  const targetWidth = Math.max(MIN_IMAGE_WIDTH, Math.min(profile.width, originalWidth))
  const targetHeight = shouldForceSquare
    ? targetWidth
    : Math.max(1, Math.round(originalHeight * (targetWidth / originalWidth)))
  const query = picsumMatch[4] || ''
  return `${picsumMatch[1]}${targetWidth}/${targetHeight}${query}`
}

function normalizeImageLikeUrl(value, path = []) {
  if (typeof value !== 'string' || !value) return value
  const apiOrigin = safeOrigin(API_BASE_URL)
  const apiBase = trimTailSlash(API_BASE_URL)
  const target = value.trim()

  if (target.startsWith('/api/')) {
    return appendResizeParamIfNeeded(`${apiBase}${target}`, path)
  }

  // 兼容历史数据中的绝对地址（包括 localhost/127.0.0.1/旧IP），统一回当前 API 域名
  const absoluteApiPathMatch = target.match(/^https?:\/\/[^/]+(\/api\/[^?#]+(?:\?[^#]*)?)$/i)
  if (absoluteApiPathMatch && absoluteApiPathMatch[1]) {
    return appendResizeParamIfNeeded(`${apiOrigin}${absoluteApiPathMatch[1]}`, path)
  }

  const localhostMatch = target.match(/^https?:\/\/(localhost|127\.0\.0\.1)(?::\d+)?(\/.*)$/i)
  if (localhostMatch && localhostMatch[2]) {
    return appendResizeParamIfNeeded(`${apiOrigin}${localhostMatch[2]}`, path)
  }

  const resized = appendResizeParamIfNeeded(target, path)
  return optimizeKnownImageCdn(resized, path)
}

function normalizePayloadUrls(payload, path = []) {
  if (Array.isArray(payload)) {
    return payload.map((item, index) => normalizePayloadUrls(item, path.concat(index)))
  }
  if (payload && typeof payload === 'object') {
    const next = {}
    Object.keys(payload).forEach((key) => {
      next[key] = normalizePayloadUrls(payload[key], path.concat(key))
    })
    return next
  }
  return normalizeImageLikeUrl(payload, path)
}

async function ensureTokenReady(requireAuth) {
  let token = getToken()
  if (token) return token
  if (!requireAuth) return ''

  try {
    const app = typeof getApp === 'function' ? getApp() : null
    if (app && typeof app.silentLogin === 'function') {
      await app.silentLogin(true)
      token = getToken()
    }
  } catch (_) {}

  if (!token) {
    throw new Error('请先完成微信登录')
  }
  return token
}

function request(options) {
  const { url, method = 'GET', data = {}, requireAuth = true, withRequestId } = options
  const requestUrl = `${API_BASE_URL}${url}`

  const sendRequest = (allowRetryOn401 = true) =>
    ensureTokenReady(requireAuth).then((token) => new Promise((resolve, reject) => {
      const header = {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      }

      if (token) {
        header.Authorization = `Bearer ${token}`
      }

      if ((withRequestId ?? shouldAttachRequestId(method)) === true) {
        header['X-Request-Id'] = createRequestId()
      }

      wx.request({
        url: requestUrl,
        method,
        data,
        header,
        success(res) {
          try {
            if (res.statusCode === 401) {
              if (requireAuth && allowRetryOn401) {
                clearToken()
                const app = typeof getApp === 'function' ? getApp() : null
                if (app && typeof app.silentLogin === 'function') {
                  Promise.resolve(app.silentLogin(true))
                    .then((ok) => {
                      if (ok === false) {
                        reject(new Error('登录已过期，请重新登录'))
                        return
                      }
                      sendRequest(false).then(resolve).catch(reject)
                    })
                    .catch(() => reject(new Error('登录已过期，请重新登录')))
                  return
                }
              }
              clearToken()
              reject(new Error('登录已过期，请重新登录'))
              return
            }

            const payload = assertApiSuccess(res.data)
            resolve(normalizePayloadUrls(payload))
          } catch (error) {
            reject(error)
          }
        },
        fail(error) {
          reject(new Error(error.errMsg || '网络请求失败'))
        },
      })
    }))

  return sendRequest(true)
}

module.exports = {
  request,
}
