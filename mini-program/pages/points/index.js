const { getPointAccount, getPointLedger } = require('../../services/points')

const BIZ_TYPE_TEXT_MAP = {
  INIT: '初始碎片',
  MANUAL_ADJUST: '手动调整',
  EXCHANGE_ORDER: '兑换扣减',
  ORDER_CANCEL_REFUND: '取消订单返还',
  ORDER_REJECT_REFUND: '驳回订单返还',
  ORDER_CLOSE_REFUND: '订单关闭返还',
  ORDER_REFUND: '订单返还',
  REGISTER_GIFT: '注册赠送',
  DAILY_SIGN: '每日签到',
}

function mapBizTypeText(code) {
  const key = String(code || '').trim().toUpperCase()
  if (!key) return '-'
  return BIZ_TYPE_TEXT_MAP[key] || key
}

function parsePage(payload) {
  if (Array.isArray(payload)) {
    return { list: payload, pageNo: 1, pageSize: payload.length || 20, total: payload.length }
  }
  if (payload && Array.isArray(payload.list)) {
    return {
      list: payload.list,
      pageNo: Number(payload.pageNo || payload.page_no || 1),
      pageSize: Number(payload.pageSize || payload.page_size || 20),
      total: Number(payload.total || payload.total_count || payload.list.length || 0),
    }
  }
  return { list: [], pageNo: 1, pageSize: 20, total: 0 }
}

function formatAmountText(amount) {
  const value = Number(amount || 0)
  if (!Number.isFinite(value)) return '0'
  if (Number.isInteger(value)) return String(value)
  return String(Math.round(value * 100) / 100)
}

function extractHitPrizeNames(rawRemark = '') {
  const text = String(rawRemark || '')
  const result = []
  const regex = /抽中[：:]?\s*([^，,；;\n\r+]+)/g
  let matched = regex.exec(text)
  while (matched) {
    const name = String(matched[1] || '').trim()
    if (name) result.push(name)
    matched = regex.exec(text)
  }
  return result
}

function alignHitPrizeNames(names = [], drawCount = 0) {
  const list = Array.isArray(names) ? names : []
  const expected = Math.max(0, Number(drawCount || 0))
  if (expected <= 0 || list.length <= expected) return list
  // 历史备注可能会在前面多拼一个“抽中：xx”，优先保留末尾实际抽奖明细。
  return list.slice(list.length - expected)
}

function extractDrawCount(rawRemark = '') {
  const matched = String(rawRemark || '').match(/抽奖次数\s*[:：]?\s*(\d+)/)
  if (!matched || !matched[1]) return 0
  const value = Number(matched[1])
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 0
}

function buildHitSummary(rawRemark = '', options = {}) {
  const fallbackCount = Math.max(0, Number(options.drawCount || 0))
  const names = alignHitPrizeNames(extractHitPrizeNames(rawRemark), fallbackCount)
  const counts = new Map()
  names.forEach((name) => {
    const prev = Number(counts.get(name) || 0)
    counts.set(name, prev + 1)
  })

  const fallbackName = String(options.prizeName || '').trim()
  if (!counts.size && fallbackName) {
    counts.set(fallbackName, fallbackCount > 1 ? fallbackCount : 1)
  }

  if (counts.size === 1 && fallbackCount > 1) {
    const onlyName = Array.from(counts.keys())[0]
    if (onlyName) {
      counts.set(onlyName, Math.max(Number(counts.get(onlyName) || 0), fallbackCount))
    }
  }

  if (!counts.size) return ''
  const text = Array.from(counts.entries())
    .map(([name, count]) => `${name}${count > 1 ? ` x${count}` : ''}`)
    .join('、')
  return text ? `抽中：${text}` : ''
}

function pickManualDrawCount(item = {}, rawRemark = '') {
  const direct = Number(item.draw_count ?? item.drawCount ?? 0)
  if (Number.isFinite(direct) && direct > 0) return Math.floor(direct)
  const parsed = extractDrawCount(rawRemark)
  if (parsed > 0) return parsed
  const hitNames = extractHitPrizeNames(rawRemark)
  return hitNames.length > 1 ? hitNames.length : 0
}

function normalizeLedger(item = {}) {
  const amount = Number(item.change_amount ?? item.point_change_amount ?? item.point_amount ?? 0)
  const bizTypeCode = item.biz_type_code || item.biz_scene_code || '-'
  const rawRemark = String(item.remark || item.note || '').trim()
  const isManualAdjust = String(bizTypeCode || '').toUpperCase() === 'MANUAL_ADJUST'
  const parsedDrawCount = extractDrawCount(rawRemark)

  let projectText = rawRemark.includes('｜')
    ? rawRemark.split('｜').pop()
    : (rawRemark.includes('：') ? rawRemark.split('：').pop() : rawRemark)
  let detailText = ''
  let hasHitSummary = false

  if (isManualAdjust) {
    const drawCount = pickManualDrawCount(item, rawRemark)
    const hitSummary = buildHitSummary(rawRemark, {
      prizeName: item.prize_name,
      drawCount,
    })
    projectText = hitSummary || projectText || '手动调整'
    detailText = `${amount >= 0 ? '+' : ''}${formatAmountText(amount)}碎片`
    hasHitSummary = !!hitSummary
  } else if (parsedDrawCount > 0 || rawRemark.includes('抽中')) {
    const hitSummary = buildHitSummary(rawRemark, { drawCount: parsedDrawCount })
    projectText = hitSummary || `抽奖记录${parsedDrawCount > 1 ? ` x${parsedDrawCount}` : ''}`
    detailText = `${amount >= 0 ? '+' : ''}${formatAmountText(amount)}碎片`
    hasHitSummary = !!hitSummary
  }

  return {
    id: item.id || item.ledger_id || '',
    biz_type_code: bizTypeCode,
    biz_type_text: mapBizTypeText(bizTypeCode),
    project_text: String(projectText || '').trim(),
    change_amount: amount,
    is_income: amount >= 0,
    balance_after: item.balance_after ?? item.point_balance_after ?? '-',
    occurred_at: item.occurred_at || item.created_at || '-',
    remark: rawRemark,
    detail_text: detailText,
    has_hit_summary: hasHitSummary,
  }
}

function normalizeAccount(item = {}) {
  return {
    point_balance: item.point_balance ?? 0,
    point_total_income: item.point_total_income ?? 0,
    point_total_expense: item.point_total_expense ?? 0,
  }
}

Page({
  data: {
    loading: false,
    loading_more: false,
    account: null,
    list: [],
    page_no: 1,
    page_size: 20,
    total: 0,
    has_more: true,
  },

  onShow() {
    this.refreshPage()
  },

  onPullDownRefresh() {
    Promise.resolve(this.refreshPage()).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    this.loadLedger({ reset: false })
  },

  async refreshPage() {
    this.setData({ loading: true })
    try {
      await Promise.all([this.loadAccount(), this.loadLedger({ reset: true })])
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadAccount() {
    try {
      const account = await getPointAccount()
      this.setData({ account: normalizeAccount(account) })
    } catch (error) {
      this.setData({ account: null })
    }
  },

  async loadLedger({ reset = false } = {}) {
    const nextPage = reset ? 1 : this.data.page_no + 1
    if (!reset && (!this.data.has_more || this.data.loading_more || this.data.loading)) return

    this.setData(reset ? { loading: true } : { loading_more: true })
    try {
      const pageData = parsePage(await getPointLedger({ pageNo: nextPage, pageSize: this.data.page_size }))
      const list = pageData.list.map(normalizeLedger)
      const mergedList = reset ? list : this.data.list.concat(list)
      this.setData({
        list: mergedList,
        page_no: pageData.pageNo,
        total: pageData.total,
        has_more: mergedList.length < pageData.total,
      })
    } catch (error) {
      wx.showToast({ title: error.message || '碎片流水加载失败', icon: 'none' })
      if (reset) this.setData({ list: [], has_more: false, total: 0 })
    } finally {
      this.setData({ loading: false, loading_more: false })
    }
  },
})
