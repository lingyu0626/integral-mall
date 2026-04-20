export type PathParams = Record<string, string | number>

export function compilePath(template: string, params: PathParams = {}): string {
  return template.replace(/\{([^}]+)\}/g, (_, key: string) => {
    const value = params[key]
    if (value === undefined || value === null) {
      throw new Error(`缺少路径参数: ${key}`)
    }
    return encodeURIComponent(String(value))
  })
}
