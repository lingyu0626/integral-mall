const { getHomeRecommends } = require('../../services/home')
const { searchProducts } = require('../../services/products')

function normalizeProduct(item = {}) {
  return {
    id: item.id || item.product_id || item.spu_id || '',
    product_name: item.product_name || item.name || item.spu_name || '未命名商品',
    main_image_url: item.main_image_url || item.image_url || item.cover_url || '',
    point_price: item.point_price || item.exchange_point || item.need_point || 0,
    stock_available: item.stock_available ?? item.stock ?? item.stock_qty ?? '-',
  }
}

function parseList(payload) {
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.list)) return payload.list
  return []
}

function normalizeBanner(banner = {}) {
  if (typeof banner === 'string') return banner
  return (
    banner.image_url ||
    banner.main_image_url ||
    banner.banner_url ||
    banner.cover_url ||
    banner.url ||
    ''
  )
}

function parseBannerList(payload) {
  const source =
    payload?.banner_list ||
    payload?.banners ||
    payload?.carousel ||
    payload?.slides ||
    []
  if (!Array.isArray(source)) return []
  return source.map(normalizeBanner).filter(Boolean)
}

Page({
  data: {
    loading: false,
    keyword: '',
    mode: 'recommend',
    list: [],
    empty_text: '暂无推荐商品',
    user_card: {
      id: '--',
      point_balance: '--',
      phone_masked: '--',
    },
    banner_list: [],
  },

  onShow() {
    this.loadRecommend()
  },

  onPullDownRefresh() {
    const task = this.data.mode === 'search' ? this.loadSearch() : this.loadRecommend()
    Promise.resolve(task).finally(() => wx.stopPullDownRefresh())
  },

  onInputKeyword(event) {
    this.setData({ keyword: event.detail.value })
  },

  async onTapSearch() {
    await this.loadSearch()
  },

  async onTapReset() {
    this.setData({ keyword: '', mode: 'recommend' })
    await this.loadRecommend()
  },

  async loadRecommend() {
    this.setData({ loading: true, empty_text: '暂无推荐商品', mode: 'recommend' })
    try {
      const data = await getHomeRecommends()
      const list = parseList(data).map(normalizeProduct).slice(0, 14)
      const bannerList = parseBannerList(data)

      const userCard = {
        id: data?.user_id || data?.user?.id || '--',
        point_balance: data?.point_balance ?? data?.point_account?.point_balance ?? '--',
        phone_masked: data?.phone_masked || data?.user?.phone_masked || '--',
      }

      this.setData({
        list,
        user_card: userCard,
        banner_list: bannerList,
      })
    } catch (error) {
      wx.showToast({ title: error.message || '加载失败', icon: 'none' })
      this.setData({ list: [], banner_list: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadSearch() {
    const keyword = String(this.data.keyword || '').trim()
    if (!keyword) {
      wx.showToast({ title: '请输入关键词', icon: 'none' })
      return
    }

    this.setData({ loading: true, empty_text: '未找到相关商品', mode: 'search' })
    try {
      const data = await searchProducts({ keyword, pageNo: 1, pageSize: 20 })
      const list = parseList(data).map(normalizeProduct)
      this.setData({ list })
    } catch (error) {
      wx.showToast({ title: error.message || '搜索失败', icon: 'none' })
      this.setData({ list: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  noop() {},

  onTapWish() {
    wx.navigateTo({ url: '/pages/wish/index' })
  },

  onTapDetail(event) {
    const { id } = event.currentTarget.dataset
    if (!id) {
      wx.showToast({ title: '商品ID缺失', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/product-detail/index?productId=${id}` })
  },
})
