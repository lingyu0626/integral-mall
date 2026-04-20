<script setup lang="ts">
import { NPagination, NSelect, NSpace, NText } from 'naive-ui'

const props = withDefaults(
  defineProps<{
    pageNo: number
    pageSize: number
    total: number
    pageSizes?: number[]
  }>(),
  {
    pageSizes: () => [10, 20, 50, 100],
  },
)

const emit = defineEmits<{
  'update:pageNo': [value: number]
  'update:pageSize': [value: number]
}>()

const pageSizeOptions = props.pageSizes.map((value) => ({
  label: `${value} / 页`,
  value,
}))
</script>

<template>
  <NSpace align="center" justify="space-between" style="width: 100%">
    <NText depth="3">共 {{ total }} 条</NText>
    <NSpace align="center" :size="12">
      <NSelect
        :value="pageSize"
        :options="pageSizeOptions"
        style="width: 110px"
        @update:value="(value) => emit('update:pageSize', value)"
      />
      <NPagination
        :item-count="total"
        :page="pageNo"
        :page-size="pageSize"
        @update:page="(value) => emit('update:pageNo', value)"
      />
    </NSpace>
  </NSpace>
</template>
