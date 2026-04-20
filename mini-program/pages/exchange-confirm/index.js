const { getProductDetail, getExchangePreview } = require('../../services/products')
const { getAddresses } = require('../../services/addresses')
const { submitExchangeOrder } = require('../../services/exchanges')
const { removeCartProduct } = require('../../utils/cart')

function parseList(payload) {
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.list)) return payload.list
  return []
}

function normalizeProduct(item = {}) {
  const rawSkuList = item.sku_list || item.skus || item.sku_options || []
  const skuList = Array.isArray(rawSkuList)
    ? rawSkuList
        .map((sku, index) => ({
          id: String(sku.sku_id || sku.id || `sku-${index + 1}`),
          sku_name: sku.sku_name || sku.spec_text || sku.name || `规格${index + 1}`,
          point_price: Number(sku.point_price ?? sku.exchange_point ?? sku.need_point ?? 0),
          stock_available: Number(sku.stock_available ?? sku.stock ?? 0),
          status_code: String(sku.status_code || sku.sale_status_code || 'ENABLED').toUpperCase(),
        }))
        .map((sku) => ({
          ...sku,
          selectable: sku.status_code === 'ENABLED' && Number(sku.stock_available || 0) > 0,
        }))
    : []

  const base = {
    id: item.id || item.product_id || item.spu_id || '',
    sku_id: String(item.sku_id || item.default_sku_id || ''),
    product_name: item.product_name || item.name || '未命名商品',
    main_image_url: item.main_image_url || item.image_url || '',
    point_price: item.point_price || item.exchange_point || item.need_point || 0,
    product_type_code: item.product_type_code || '',
    stock_available: Number(item.stock_available ?? item.stock ?? 0),
    sku_list: skuList,
    sku_name: '',
  }
  return applySelectedSku(base, base.sku_id)
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

function normalizePreview(item = {}) {
  return {
    can_exchange: item.can_exchange ?? true,
    reason: item.reason || item.block_reason || '满足兑换条件',
    point_balance: item.point_balance ?? '-',
    required_point: item.required_point ?? item.point_price ?? '-',
    require_address: item.require_address ?? item.need_address ?? item.require_delivery_address ?? false,
  }
}

function normalizeAddress(item = {}) {
  return {
    id: item.id || item.address_id || '',
    receiver_name: item.receiver_name || '',
    receiver_phone: item.receiver_phone || '',
    province_name: item.province_name || '',
    city_name: item.city_name || '',
    district_name: item.district_name || '',
    detail_address: item.detail_address || '',
    is_default: Number(item.is_default || 0) === 1,
  }
}

Page({
  data: {
    loading: false,
    submitting: false,
    product_id: '',
    product: null,
    selected_sku_id: '',
    preview: null,
    address_list: [],
    selected_address: null,
    selected_address_id: '',
    user_remark: '',
  },

  onLoad(options) {
    const productId = options.productId || ''
    const skuId = options.skuId || ''
    this.setData({ product_id: productId, selected_sku_id: String(skuId || '') })
    this.loadPage()
  },

  onShow() {
    if (!this.data.product_id) return
    this.loadAddressList()
  },

  async loadPage() {
    const productId = this.data.product_id
    if (!productId) {
      wx.showToast({ title: '缺少 productId', icon: 'none' })
      return
    }

    this.setData({ loading: true })
    try {
      const detail = await getProductDetail(productId)
      const normalized = normalizeProduct(detail)
      const product = applySelectedSku(normalized, this.data.selected_sku_id || normalized.sku_id)
      const previewData = await getExchangePreview(productId, { sku_id: product.sku_id }).catch(() => null)
      const preview = previewData ? normalizePreview(previewData) : null

      this.setData({
        product,
        selected_sku_id: product.sku_id || '',
        preview,
      })
      await this.loadAddressList()
    } catch (error) {
      wx.showToast({ title: error.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadAddressList() {
    try {
      const addressesData = await getAddresses({ pageNo: 1, pageSize: 100 }).catch(() => [])
      const addressList = parseList(addressesData).map(normalizeAddress)
      const currentSelectedId = String(this.data.selected_address_id || '')
      const existsCurrent = addressList.some((item) => String(item.id) === currentSelectedId)
      const selectedAddressId = existsCurrent
        ? this.data.selected_address_id
        : (addressList.find((item) => item.is_default)?.id || addressList[0]?.id || '')
      const selectedAddress = addressList.find((item) => String(item.id) === String(selectedAddressId)) || null

      this.setData({
        address_list: addressList,
        selected_address: selectedAddress,
        selected_address_id: selectedAddressId,
      })
    } catch (_) {}
  },

  onTapSelectAddress(event) {
    const { id } = event.currentTarget.dataset
    if (!id) return
    this.setData({ selected_address_id: id })
  },

  onTapGotoAddress() {
    const selectedAddressId = String(this.data.selected_address_id || '')
    wx.navigateTo({
      url: `/pages/address/index?mode=select&selectedAddressId=${encodeURIComponent(selectedAddressId)}`,
      events: {
        addressSelected: (address) => {
          if (!address || !address.id) return
          const normalized = normalizeAddress(address)
          const nextList = (this.data.address_list || []).filter((item) => String(item.id) !== String(normalized.id))
          nextList.unshift(normalized)
          this.setData({
            address_list: nextList,
            selected_address: normalized,
            selected_address_id: normalized.id,
          })
        },
      },
    })
  },

  onInputRemark(event) {
    this.setData({ user_remark: event.detail.value })
  },

  async onTapSelectSku(event) {
    const skuId = String(event?.currentTarget?.dataset?.id || '')
    const selectable = event?.currentTarget?.dataset?.selectable
    if (!skuId || !this.data.product || selectable === false || selectable === 'false') return
    const product = applySelectedSku(this.data.product, skuId)
    this.setData({
      product,
      selected_sku_id: product.sku_id || '',
    })
    try {
      const previewData = await getExchangePreview(this.data.product_id, { sku_id: product.sku_id })
      this.setData({ preview: normalizePreview(previewData || {}) })
    } catch (_) {}
  },

  shouldRequireAddress() {
    if (this.data.preview && this.data.preview.require_address !== undefined) {
      return !!this.data.preview.require_address
    }
    const typeCode = String(this.data.product?.product_type_code || '').toUpperCase()
    return typeCode.includes('PHYSICAL') || typeCode.includes('ENTITY') || typeCode.includes('GOODS')
  },

  async onTapSubmitExchange() {
    if (this.data.submitting || this.data.loading) return
    if (!this.data.product?.id) return

    if (this.shouldRequireAddress() && !this.data.selected_address_id) {
      wx.showToast({ title: '实物商品请选择收货地址', icon: 'none' })
      return
    }

    this.setData({ submitting: true })

    try {
      const payload = {
        spu_id: this.data.product.id,
        sku_id: this.data.selected_sku_id || this.data.product.sku_id || undefined,
        quantity: 1,
        address_id: this.data.selected_address_id || undefined,
        user_remark: String(this.data.user_remark || '').trim() || undefined,
        source_scene_code: 'WX_MINI_PROGRAM',
      }
      const data = await submitExchangeOrder(payload)
      const orderNo = data?.order_no || data?.order?.order_no || ''
      const routeProductId = String(this.data.product_id || '').trim()
      const detailProductId = String(this.data.product?.id || '').trim()
      if (routeProductId) {
        removeCartProduct(routeProductId)
      }
      if (detailProductId && detailProductId !== routeProductId) {
        removeCartProduct(detailProductId)
      }
      wx.showModal({
        title: '兑换提交成功',
        content: orderNo ? `订单号：${orderNo}` : '已提交，请到我的订单查看状态',
        showCancel: false,
        success: () => wx.redirectTo({ url: '/pages/orders/index' }),
      })
    } catch (error) {
      wx.showToast({ title: error.message || '提交失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
