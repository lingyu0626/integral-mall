function compilePath(template, params = {}) {
  return String(template).replace(/\{([^}]+)\}/g, (_, key) => {
    const value = params[key]
    if (value === undefined || value === null || value === '') {
      throw new Error(`缺少路径参数: ${key}`)
    }
    return encodeURIComponent(String(value))
  })
}

module.exports = {
  compilePath,
}
