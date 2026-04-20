import { describe, expect, it } from 'vitest'
import { apiTableMappings, day1Docs, projectScope } from '../src/data/day1Baseline'
import { countByOwner, getUniqueTableCount, hasUniqueApiPrefix } from '../src/utils/day1Metrics'

describe('Day1 baseline data integrity', () => {
  it('API 模块总量和项目范围一致', () => {
    expect(apiTableMappings.length).toBe(projectScope.appApiGroupCount + projectScope.adminApiGroupCount)
  })

  it('API 前缀唯一，不重复定义', () => {
    expect(hasUniqueApiPrefix(apiTableMappings)).toBe(true)
  })

  it('所有表名遵循 pm_ 前缀命名', () => {
    const allTables = apiTableMappings.flatMap((item) => item.tables)
    expect(allTables.every((table) => table.startsWith('pm_'))).toBe(true)
  })

  it('负责人分配与计划一致（FE-MP / FE-ADMIN）', () => {
    const ownerCount = countByOwner(apiTableMappings)
    expect(ownerCount['FE-MP']).toBe(projectScope.appApiGroupCount)
    expect(ownerCount['FE-ADMIN']).toBe(projectScope.adminApiGroupCount)
  })

  it('文档基线在 Day1 已全部确认', () => {
    expect(day1Docs.every((doc) => doc.status === '已确认')).toBe(true)
    expect(day1Docs).toHaveLength(4)
  })

  it('映射涉及的去重表数量应达到核心业务覆盖', () => {
    expect(getUniqueTableCount(apiTableMappings)).toBeGreaterThanOrEqual(24)
  })
})
