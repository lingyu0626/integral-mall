function maskPhone(phone) {
  if (!phone) return ''
  const value = String(phone)
  if (value.length < 7) return value
  return `${value.slice(0, 3)}****${value.slice(-4)}`
}

module.exports = {
  maskPhone,
}
