const { createWishDemand, getWishDemands } = require('../../services/wish')
const MAX_WISH_IMAGES = 9

function parsePage(payload) {
  if (Array.isArray(payload)) {
    return { list: payload, pageNo: 1, total: payload.length }
  }
  if (payload && Array.isArray(payload.list)) {
    return {
      list: payload.list,
      pageNo: Number(payload.pageNo || payload.page_no || 1),
      total: Number(payload.total || payload.total_count || payload.list.length || 0),
    }
  }
  return { list: [], pageNo: 1, total: 0 }
}

function parseImageUrls(item = {}) {
  const list = []
  const seen = new Set()
  const pushUrl = (value) => {
    const text = String(value || '').trim()
    if (!text || seen.has(text)) return
    seen.add(text)
    list.push(text)
  }
  const append = (value) => {
    if (Array.isArray(value)) {
      value.forEach(pushUrl)
      return
    }
    const text = String(value || '').trim()
    if (!text) return
    if (text.startsWith('[') && text.endsWith(']')) {
      try {
        const parsed = JSON.parse(text)
        if (Array.isArray(parsed)) {
          parsed.forEach(pushUrl)
          return
        }
      } catch (_) {}
    }
    pushUrl(text)
  }

  append(item.image_urls)
  pushUrl(item.image_url)
  return list.slice(0, MAX_WISH_IMAGES)
}

function normalizeItem(item = {}) {
  const statusCode = String(item.status_code || '').toUpperCase()
  const imageUrls = parseImageUrls(item)
  return {
    id: item.id || '',
    wish_title: item.wish_title || '-',
    wish_message: item.wish_message || '',
    image_url: imageUrls[0] || '',
    image_urls: imageUrls,
    status_code: statusCode,
    status_text: item.status_text || (statusCode === 'APPROVED' ? '已确认' : statusCode === 'REJECTED' ? '已拒绝' : '待处理'),
    decision_note: item.decision_note || '',
    notify_content: item.notify_content || '',
    created_at: item.created_at || '',
  }
}

function fileToDataUrl(filePath) {
  return new Promise((resolve, reject) => {
    const fs = wx.getFileSystemManager()
    fs.readFile({
      filePath,
      encoding: 'base64',
      success: (res) => {
        const ext = String(filePath || '').split('.').pop()?.toLowerCase() || 'jpg'
        const mime = ext === 'png' ? 'image/png' : ext === 'webp' ? 'image/webp' : 'image/jpeg'
        resolve(`data:${mime};base64,${res.data || ''}`)
      },
      fail: () => reject(new Error('图片读取失败')),
    })
  })
}

Page({
  data: {
    loading: false,
    submitting: false,
    choosing: false,
    list: [],
    wish_title: '',
    wish_message: '',
    image_urls: [],
    image_data_urls: [],
  },

  onShow() {
    this.loadList()
  },

  async loadList() {
    this.setData({ loading: true })
    try {
      const page = parsePage(await getWishDemands({ pageNo: 1, pageSize: 20 }))
      this.setData({ list: page.list.map(normalizeItem) })
    } catch (error) {
      wx.showToast({ title: error.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onInputTitle(e) {
    this.setData({ wish_title: e.detail.value })
  },

  onInputMessage(e) {
    this.setData({ wish_message: e.detail.value })
  },

  async onChooseImage() {
    if (this.data.choosing) return
    const remain = MAX_WISH_IMAGES - this.data.image_urls.length
    if (remain <= 0) {
      wx.showToast({ title: `最多上传${MAX_WISH_IMAGES}张`, icon: 'none' })
      return
    }
    this.setData({ choosing: true })
    try {
      const res = await new Promise((resolve, reject) => {
        wx.chooseMedia({
          count: Math.min(remain, MAX_WISH_IMAGES),
          mediaType: ['image'],
          sourceType: ['album', 'camera'],
          sizeType: ['compressed'],
          success: resolve,
          fail: reject,
        })
      })
      const tempFiles = Array.isArray(res?.tempFiles) ? res.tempFiles : []
      const selected = tempFiles
        .map((file) => file?.tempFilePath || '')
        .filter(Boolean)
      if (!selected.length) {
        this.setData({ choosing: false })
        return
      }
      const existing = new Set(this.data.image_urls)
      const nextPaths = []
      selected.forEach((path) => {
        if (nextPaths.length >= remain) return
        if (existing.has(path)) return
        existing.add(path)
        nextPaths.push(path)
      })
      if (!nextPaths.length) {
        wx.showToast({ title: '图片已添加', icon: 'none' })
        return
      }
      const nextDataUrls = await Promise.all(nextPaths.map((path) => fileToDataUrl(path)))
      this.setData({
        image_urls: this.data.image_urls.concat(nextPaths).slice(0, MAX_WISH_IMAGES),
        image_data_urls: this.data.image_data_urls.concat(nextDataUrls).slice(0, MAX_WISH_IMAGES),
      })
    } catch (_) {
      wx.showToast({ title: '选择图片失败', icon: 'none' })
    } finally {
      this.setData({ choosing: false })
    }
  },

  onRemoveImage(e) {
    const index = Number(e?.currentTarget?.dataset?.index)
    if (Number.isNaN(index) || index < 0) return
    const imageUrls = this.data.image_urls.slice()
    const imageDataUrls = this.data.image_data_urls.slice()
    if (index >= imageUrls.length) return
    imageUrls.splice(index, 1)
    if (index < imageDataUrls.length) imageDataUrls.splice(index, 1)
    this.setData({ image_urls: imageUrls, image_data_urls: imageDataUrls })
  },

  onClearImages() {
    this.setData({ image_urls: [], image_data_urls: [] })
  },

  async onSubmit() {
    const title = String(this.data.wish_title || '').trim()
    const message = String(this.data.wish_message || '').trim()
    if (!title && !message) {
      wx.showToast({ title: '请填写商品名或留言内容', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    try {
      const imageDataUrls = this.data.image_data_urls.slice(0, MAX_WISH_IMAGES)
      await createWishDemand({
        wish_title: title,
        wish_message: message,
        image_data_url: imageDataUrls[0] || '',
        image_data_urls: imageDataUrls,
      })
      wx.showToast({ title: '提交成功', icon: 'success' })
      this.setData({
        wish_title: '',
        wish_message: '',
        image_urls: [],
        image_data_urls: [],
      })
      await this.loadList()
    } catch (error) {
      wx.showToast({ title: error.message || '提交失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
