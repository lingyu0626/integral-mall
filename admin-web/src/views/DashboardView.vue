<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { NCard, NDescriptions, NDescriptionsItem, NGrid, NGridItem, NStatistic } from 'naive-ui'
import { fetchDashboardOverview } from '../api/admin/dashboard'
import {
  fetchConversionStats,
  fetchOrderStats,
  fetchProductStats,
  fetchUserStats,
} from '../api/admin/monitor'
import type { DashboardOverview } from '../mock/admin'
import type { ConversionStats, OrderStats, ProductStats, UserStats } from '../mock/monitor-center'

const loading = ref(false)
const overview = ref<DashboardOverview>({
  user_total: 0,
  order_total: 0,
  pending_audit_count: 0,
  today_exchange_point: 0,
  pending_writeoff_point: 0,
  product_total: 0,
})

const userStats = ref<UserStats>({
  new_user_today: 0,
  active_user_7d: 0,
  frozen_user_count: 0,
  conversion_rate: 0,
})

const orderStats = ref<OrderStats>({
  pending_audit_count: 0,
  pending_ship_count: 0,
  shipped_count: 0,
  finished_count: 0,
  close_count: 0,
})

const productStats = ref<ProductStats>({
  on_shelf_count: 0,
  off_shelf_count: 0,
  recommend_count: 0,
  low_stock_count: 0,
})

const conversionStats = ref<ConversionStats>({
  visit_uv_today: 0,
  exchange_uv_today: 0,
  conversion_rate: 0,
  avg_exchange_point: 0,
})

function asPercent(value: number): string {
  return `${(value * 100).toFixed(2)}%`
}

async function loadDashboard() {
  loading.value = true
  try {
    const [overviewData, userData, orderData, productData, conversionData] = await Promise.all([
      fetchDashboardOverview(),
      fetchUserStats(),
      fetchOrderStats(),
      fetchProductStats(),
      fetchConversionStats(),
    ])
    overview.value = overviewData
    userStats.value = userData
    orderStats.value = orderData
    productStats.value = productData
    conversionStats.value = conversionData
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <div class="page-wrap">
    <NGrid :x-gap="12" :y-gap="12" cols="2 s:2 m:6" responsive="screen">
      <NGridItem>
        <NCard>
          <NStatistic label="用户总量" :value="overview.user_total" />
        </NCard>
      </NGridItem>
      <NGridItem>
        <NCard>
          <NStatistic label="订单总量" :value="overview.order_total" />
        </NCard>
      </NGridItem>
      <NGridItem>
        <NCard>
          <NStatistic label="商品总数" :value="overview.product_total" />
        </NCard>
      </NGridItem>
      <NGridItem>
        <NCard>
          <NStatistic label="今日新增用户" :value="userStats.new_user_today" />
        </NCard>
      </NGridItem>
      <NGridItem>
        <NCard>
          <NStatistic label="今日兑换碎片" :value="overview.today_exchange_point" />
        </NCard>
      </NGridItem>
      <NGridItem>
        <NCard>
          <NStatistic label="待核销碎片" :value="overview.pending_writeoff_point || 0" />
        </NCard>
      </NGridItem>
    </NGrid>

    <NGrid :x-gap="12" :y-gap="12" cols="1 m:2" responsive="screen" style="margin-top: 12px">
      <NGridItem>
        <NCard title="订单监控" :loading="loading">
          <NDescriptions bordered label-placement="left" :column="2" size="small">
            <NDescriptionsItem label="待审核">{{ orderStats.pending_audit_count }}</NDescriptionsItem>
            <NDescriptionsItem label="待发货">{{ orderStats.pending_ship_count }}</NDescriptionsItem>
            <NDescriptionsItem label="已发货">{{ orderStats.shipped_count }}</NDescriptionsItem>
            <NDescriptionsItem label="已完成">{{ orderStats.finished_count }}</NDescriptionsItem>
            <NDescriptionsItem label="已关闭">{{ orderStats.close_count }}</NDescriptionsItem>
            <NDescriptionsItem label="转化率">{{ asPercent(conversionStats.conversion_rate) }}</NDescriptionsItem>
          </NDescriptions>
        </NCard>
      </NGridItem>
      <NGridItem>
        <NCard title="用户与商品监控" :loading="loading">
          <NDescriptions bordered label-placement="left" :column="2" size="small">
            <NDescriptionsItem label="7日活跃用户">{{ userStats.active_user_7d }}</NDescriptionsItem>
            <NDescriptionsItem label="冻结用户">{{ userStats.frozen_user_count }}</NDescriptionsItem>
            <NDescriptionsItem label="上架商品">{{ productStats.on_shelf_count }}</NDescriptionsItem>
            <NDescriptionsItem label="下架商品">{{ productStats.off_shelf_count }}</NDescriptionsItem>
            <NDescriptionsItem label="推荐商品">{{ productStats.recommend_count }}</NDescriptionsItem>
            <NDescriptionsItem label="低库存商品">{{ productStats.low_stock_count }}</NDescriptionsItem>
            <NDescriptionsItem label="访问UV(今日)">{{ conversionStats.visit_uv_today }}</NDescriptionsItem>
            <NDescriptionsItem label="兑换UV(今日)">{{ conversionStats.exchange_uv_today }}</NDescriptionsItem>
          </NDescriptions>
        </NCard>
      </NGridItem>
    </NGrid>
  </div>
</template>

<style scoped>
.page-wrap {
  display: flex;
  flex-direction: column;
}
</style>
