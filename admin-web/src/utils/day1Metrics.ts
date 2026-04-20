import type { ApiTableMapping } from '../data/day1Baseline'

export function getUniqueTableCount(mappings: ApiTableMapping[]): number {
  return new Set(mappings.flatMap((item) => item.tables)).size
}

export function hasUniqueApiPrefix(mappings: ApiTableMapping[]): boolean {
  return new Set(mappings.map((item) => item.apiPrefix)).size === mappings.length
}

export function countByOwner(mappings: ApiTableMapping[]): Record<string, number> {
  return mappings.reduce<Record<string, number>>((acc, item) => {
    acc[item.owner] = (acc[item.owner] ?? 0) + 1
    return acc
  }, {})
}
