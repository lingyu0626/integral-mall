const { createAddress, updateAddress, getAddressDetail } = require('../../services/addresses')

function createEmptyForm() {
  return {
    receiver_name: '',
    receiver_phone: '',
    province_name: '',
    city_name: '',
    district_name: '',
    detail_address: '',
    is_default: false,
  }
}

function normalizeAddress(item = {}) {
  return {
    receiver_name: item.receiver_name || '',
    receiver_phone: item.receiver_phone || '',
    province_name: item.province_name || '',
    city_name: item.city_name || '',
    district_name: item.district_name || '',
    detail_address: item.detail_address || '',
    is_default: Number(item.is_default || 0) === 1,
  }
}

function pickName(text = '') {
  const onlyCn = String(text).match(/[\u4e00-\u9fa5]{2,6}/g)
  return onlyCn?.[0] || ''
}

Page({
  data: {
    loading: false,
    submitting: false,
    editing_id: '',
    quick_text: '',
    form: createEmptyForm(),
    region: [],
  },

  onLoad(options) {
    const addressId = options.addressId || ''
    if (!addressId) return

    this.setData({ editing_id: addressId })
    wx.setNavigationBarTitle({ title: '编辑地址' })
    this.loadAddressDetail(addressId)
  },

  async loadAddressDetail(addressId) {
    this.setData({ loading: true })
    try {
      const detail = await getAddressDetail(addressId)
      const form = normalizeAddress(detail)
      this.setData({
        form,
        region: [form.province_name, form.city_name, form.district_name].filter(Boolean),
      })
    } catch (error) {
      wx.showToast({ title: error.message || '地址加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onInputQuick(event) {
    this.setData({ quick_text: event.detail.value })
  },

  onTapRecognize() {
    const raw = String(this.data.quick_text || '').trim()
    if (!raw) {
      wx.showToast({ title: '请先粘贴地址信息', icon: 'none' })
      return
    }

    const phoneMatch = raw.match(/1\d{10}/)
    const phone = phoneMatch ? phoneMatch[0] : ''
    const name = pickName(raw)

    let detail = raw
    if (name) detail = detail.replace(name, '')
    if (phone) detail = detail.replace(phone, '')
    detail = detail.replace(/[,，;；。\n\t]+/g, ' ').trim()

    this.setData({
      'form.receiver_name': name || this.data.form.receiver_name,
      'form.receiver_phone': phone || this.data.form.receiver_phone,
      'form.detail_address': detail || this.data.form.detail_address,
    })

    wx.showToast({ title: '已识别，请补全省市区', icon: 'none' })
  },

  onInputField(event) {
    const { field } = event.currentTarget.dataset
    if (!field) return
    this.setData({ [`form.${field}`]: event.detail.value })
  },

  onPickRegion(event) {
    const value = event.detail.value || []
    this.setData({
      region: value,
      'form.province_name': value[0] || '',
      'form.city_name': value[1] || '',
      'form.district_name': value[2] || '',
    })
  },

  onToggleDefault() {
    this.setData({ 'form.is_default': !this.data.form.is_default })
  },

  async onTapSave() {
    if (this.data.submitting || this.data.loading) return

    const form = this.data.form
    if (!String(form.receiver_name || '').trim()) {
      wx.showToast({ title: '请填写收货人', icon: 'none' })
      return
    }
    if (!/^1\d{10}$/.test(String(form.receiver_phone || '').trim())) {
      wx.showToast({ title: '请填写11位手机号', icon: 'none' })
      return
    }
    if (!form.province_name || !form.city_name || !form.district_name) {
      wx.showToast({ title: '请选择区域', icon: 'none' })
      return
    }
    if (!String(form.detail_address || '').trim()) {
      wx.showToast({ title: '请填写详细地址', icon: 'none' })
      return
    }

    const payload = {
      receiver_name: String(form.receiver_name || '').trim(),
      receiver_phone: String(form.receiver_phone || '').trim(),
      province_name: String(form.province_name || '').trim(),
      city_name: String(form.city_name || '').trim(),
      district_name: String(form.district_name || '').trim(),
      detail_address: String(form.detail_address || '').trim(),
      is_default: form.is_default ? 1 : 0,
      country_code: 'CN',
      status_code: 'ACTIVE',
    }

    this.setData({ submitting: true })
    try {
      if (this.data.editing_id) {
        await updateAddress(this.data.editing_id, payload)
      } else {
        await createAddress(payload)
      }
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 380)
    } catch (error) {
      wx.showToast({ title: error.message || '保存失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
