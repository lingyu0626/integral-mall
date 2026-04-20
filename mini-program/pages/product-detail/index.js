const { getProductDetail } = require('../../services/products')
const { addProductToCart } = require('../../utils/cart')

function decodeHtmlEntities(raw = '') {
  let text = String(raw || '')
  // 处理多层 &amp;amp; 场景
  for (let i = 0; i < 5; i += 1) {
    const next = text
      .replace(/&amp;/g, '&')
      .replace(/&nbsp;/g, ' ')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'")
    if (next === text) break
    text = next
  }
  return text
}

function stripHtml(raw = '') {
  return String(raw || '')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<[^>]+>/g, '')
}

function normalizeSku(item = {}, index = 0) {
  const id = item.sku_id || item.id || `sku-${index + 1}`
  const statusCode = String(item.status_code || item.sale_status_code || 'ENABLED').toUpperCase()
  const stock = Number(item.stock_available ?? item.stock ?? 0)
  const skuName = item.sku_name || item.spec_text || item.name || `规格${index + 1}`
  const skuImageUrl = item.image_url || item.main_image_url || item.sku_image_url || ''
  return {
    id: String(id),
    sku_name: skuName,
    point_price: Number(item.point_price ?? item.exchange_point ?? item.need_point ?? 0),
    stock_available: Math.max(0, stock),
    status_code: statusCode,
    selectable: statusCode === 'ENABLED' && stock > 0,
    image_url: String(skuImageUrl || '').trim(),
  }
}

function normalizeImageDedupKey(value = '') {
  const text = String(value || '').trim()
  if (!text) return ''
  const pathMatch = text.match(/^https?:\/\/[^/]+(\/api\/v1\/admin\/files\/\d+\/content)(?:[?#].*)?$/i)
  if (pathMatch && pathMatch[1]) return pathMatch[1]
  const relativeMatch = text.match(/^(\/api\/v1\/admin\/files\/\d+\/content)(?:[?#].*)?$/i)
  if (relativeMatch && relativeMatch[1]) return relativeMatch[1]
  return text
}

function collectImageUrls(item = {}) {
  const list = []
  const seen = new Set()

  const push = (value) => {
    const text = String(value || '').trim()
    if (!text) return
    const key = normalizeImageDedupKey(text)
    if (!key || seen.has(key)) return
    seen.add(key)
    list.push(text)
  }
  const append = (value) => {
    if (!value) return
    if (Array.isArray(value)) {
      value.forEach((item) => push(item))
      return
    }
    const text = String(value || '').trim()
    if (!text) return
    if (text.startsWith('[') && text.endsWith(']')) {
      try {
        const parsed = JSON.parse(text)
        if (Array.isArray(parsed)) {
          parsed.forEach((item) => push(item))
          return
        }
      } catch (_) {}
    }
    push(text)
  }

  append(item.image_urls)
  if (Array.isArray(item.media_list)) {
    item.media_list.forEach((media) => {
      if (!media) return
      const mediaType = String(media.media_type || 'IMAGE').toUpperCase()
      if (mediaType !== 'IMAGE') return
      push(media.media_url)
    })
  }
  push(item.main_image_url)
  push(item.image_url)
  return list
}

function findImageIndex(imageUrls = [], imageUrl = '') {
  if (!Array.isArray(imageUrls) || !imageUrls.length) return -1
  const targetKey = normalizeImageDedupKey(imageUrl)
  if (!targetKey) return -1
  for (let i = 0; i < imageUrls.length; i += 1) {
    if (normalizeImageDedupKey(imageUrls[i]) === targetKey) return i
  }
  return -1
}

function resolveSkuBannerIndex(product = {}, sku = null, fallbackIndex = 0) {
  const imageUrls = Array.isArray(product.image_urls) ? product.image_urls : []
  if (!imageUrls.length) return 0

  const matchedByImage = findImageIndex(imageUrls, sku?.image_url || sku?.main_image_url || sku?.sku_image_url || '')
  if (matchedByImage >= 0) return matchedByImage

  const skuList = Array.isArray(product.sku_list) ? product.sku_list : []
  const selectedSkuId = String(sku?.id || '')
  if (selectedSkuId) {
    const matchedByIndex = skuList.findIndex((item) => String(item?.id || '') === selectedSkuId)
    if (matchedByIndex >= 0 && matchedByIndex < imageUrls.length) return matchedByIndex
  }

  const safeFallback = Number(fallbackIndex)
  if (Number.isFinite(safeFallback) && safeFallback >= 0 && safeFallback < imageUrls.length) return safeFallback
  return 0
}

function applySelectedSku(product = {}, desiredSkuId = '') {
  const skuList = Array.isArray(product.sku_list) ? product.sku_list : []
  if (!skuList.length) {
    return {
      ...product,
      sku_id: '',
      sku_name: '',
    }
  }

  const desired = String(desiredSkuId || '')
  const selected =
    skuList.find((item) => String(item.id) === desired) ||
    skuList.find((item) => item.selectable) ||
    skuList[0]

  if (!selected) return product

  return {
    ...product,
    sku_id: selected.id,
    sku_name: selected.sku_name,
    point_price: Number(selected.point_price ?? product.point_price ?? 0),
    stock_available: Number(selected.stock_available ?? product.stock_available ?? 0),
  }
}

function normalizeProduct(item = {}) {
  const detailSource = item.detail_html || item.detail || '暂无详情描述'
  const detailText = stripHtml(decodeHtmlEntities(detailSource)).replace(/\n{2,}/g, '\n').trim()
  const rawSkuList =
    item.sku_list ||
    item.skus ||
    item.sku_options ||
    []
  const skuList = Array.isArray(rawSkuList) ? rawSkuList.map(normalizeSku).filter((sku) => sku.id) : []
  const imageUrls = collectImageUrls(item)
  const mainImage = imageUrls[0] || item.main_image_url || item.image_url || ''
  if (!imageUrls.length && mainImage) imageUrls.push(mainImage)

  const base = {
    id: item.id || item.product_id || item.spu_id || '',
    product_name: item.product_name || item.name || '未命名商品',
    main_image_url: mainImage,
    image_urls: imageUrls,
    detail_text: detailText || '暂无详情描述',
    point_price: item.point_price || item.exchange_point || item.need_point || 0,
    stock_available: item.stock_available ?? item.stock ?? '-',
    limit_per_user: item.limit_per_user ?? item.limit_count ?? '-',
    product_type_code: item.product_type_code || item.type_code || '-',
    sku_list: skuList,
    sku_id: item.sku_id || item.default_sku_id || '',
    sku_name: '',
  }

  return applySelectedSku(base, base.sku_id)
}

Page({
  data: {
    loading: false,
    product_id: '',
    product: null,
    banner_current: 0,
  },

  onLoad(options) {
    const productId = options.productId || ''
    this.setData({ product_id: productId })
    this.loadPage(productId)
  },

  async loadPage(productId) {
    if (!productId) {
      wx.showToast({ title: '缺少 productId', icon: 'none' })
      return
    }

    this.setData({ loading: true })

    try {
      const detail = await getProductDetail(productId)
      const product = normalizeProduct(detail)
      const selectedSku =
        (Array.isArray(product.sku_list) && product.sku_list.find((item) => String(item.id) === String(product.sku_id || ''))) || null
      const bannerCurrent = resolveSkuBannerIndex(product, selectedSku, 0)
      this.setData({ product, banner_current: bannerCurrent })
    } catch (error) {
      wx.showToast({ title: error.message || '详情加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onTapHome() {
    wx.switchTab({ url: '/pages/home/index' })
  },

  onTapBackpack() {
    if (!this.data.product || !this.data.product.id) {
      wx.showToast({ title: '商品信息缺失', icon: 'none' })
      return
    }

    wx.showModal({
      title: '加入背包',
      content: '确认将该商品加入背包吗？可按加入时间筛选查看。',
      confirmText: '加入',
      success: ({ confirm }) => {
        if (!confirm) return
        try {
          addProductToCart({
            product_id: this.data.product.id,
            sku_id: this.data.product.sku_id || '',
            sku_name: this.data.product.sku_name || '',
            product_name: this.data.product.product_name,
            main_image_url: this.data.product.main_image_url,
            point_price: this.data.product.point_price,
          })
          wx.showToast({ title: '已加入背包', icon: 'success' })
        } catch (error) {
          wx.showToast({ title: error.message || '加入背包失败', icon: 'none' })
        }
      },
    })
  },

  onTapExchange() {
    if (!this.data.product_id) {
      wx.showToast({ title: '缺少商品ID', icon: 'none' })
      return
    }
    const skuId = this.data.product?.sku_id ? `&skuId=${encodeURIComponent(this.data.product.sku_id)}` : ''
    wx.navigateTo({ url: `/pages/exchange-confirm/index?productId=${this.data.product_id}${skuId}` })
  },

  onTapSku(event) {
    const id = event?.currentTarget?.dataset?.id
    const selectable = event?.currentTarget?.dataset?.selectable
    const index = Number(event?.currentTarget?.dataset?.index)
    if (!id || !this.data.product || selectable === false || selectable === 'false') return
    const next = applySelectedSku(this.data.product, id)
    const selectedSku = Array.isArray(next.sku_list) ? next.sku_list.find((item) => String(item.id) === String(id)) : null
    const nextBannerCurrent = resolveSkuBannerIndex(next, selectedSku, index)
    this.setData({ product: next, banner_current: nextBannerCurrent })
  },

  onBannerChange(event) {
    this.setData({ banner_current: Number(event?.detail?.current || 0) })
  },
})
