const CART_STORAGE_KEY = 'mall_cart_items_v1'

function pad2(num) {
  return String(num).padStart(2, '0')
}

function formatDateTime(date = new Date()) {
  const y = date.getFullYear()
  const m = pad2(date.getMonth() + 1)
  const d = pad2(date.getDate())
  const hh = pad2(date.getHours())
  const mm = pad2(date.getMinutes())
  const ss = pad2(date.getSeconds())
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
}

function normalizeCartItem(raw = {}) {
  const id = raw.product_id || raw.id || raw.spu_id || ''
  const addedTs = Number(raw.added_ts || 0) || Date.now()
  return {
    product_id: String(id),
    sku_id: String(raw.sku_id || ''),
    sku_name: raw.sku_name || '',
    product_name: raw.product_name || raw.name || raw.spu_name || '未命名商品',
    main_image_url: raw.main_image_url || raw.image_url || raw.cover_url || '',
    point_price: Number(raw.point_price || raw.exchange_point || raw.need_point || 0),
    quantity: Math.max(1, Number(raw.quantity || 1)),
    added_ts: addedTs,
    added_at: raw.added_at || formatDateTime(new Date(addedTs)),
  }
}

function loadCartItems() {
  const raw = wx.getStorageSync(CART_STORAGE_KEY)
  if (!Array.isArray(raw)) return []
  return raw
    .map(normalizeCartItem)
    .filter((item) => item.product_id)
    .sort((a, b) => Number(b.added_ts || 0) - Number(a.added_ts || 0))
}

function saveCartItems(items = []) {
  wx.setStorageSync(CART_STORAGE_KEY, items.map(normalizeCartItem))
}

function addProductToCart(product = {}, quantity = 1) {
  const normalized = normalizeCartItem(product)
  if (!normalized.product_id) {
    throw new Error('商品ID缺失，无法加入购物车')
  }

  const list = loadCartItems()
  const hit = list.find((item) => item.product_id === normalized.product_id)
  const nowTs = Date.now()
  const nowText = formatDateTime(new Date(nowTs))

  if (hit) {
    hit.product_name = normalized.product_name
    hit.main_image_url = normalized.main_image_url
    hit.point_price = normalized.point_price
    hit.quantity = Math.max(1, Number(hit.quantity || 1) + Math.max(1, Number(quantity || 1)))
    hit.added_ts = nowTs
    hit.added_at = nowText
  } else {
    list.push({
      ...normalized,
      quantity: Math.max(1, Number(quantity || normalized.quantity || 1)),
      added_ts: nowTs,
      added_at: nowText,
    })
  }

  saveCartItems(list)
  return loadCartItems()
}

function removeCartProduct(productId) {
  const id = String(productId || '')
  if (!id) return
  const next = loadCartItems().filter((item) => item.product_id !== id)
  saveCartItems(next)
}

function clearCartItems() {
  wx.removeStorageSync(CART_STORAGE_KEY)
}

module.exports = {
  loadCartItems,
  addProductToCart,
  removeCartProduct,
  clearCartItems,
  formatDateTime,
}
