const UNIT = 1024
export function formatFileSize(size: number) {
  if (size < UNIT) {
    return `${size}B`
  }
  let tmp = size / UNIT
  if (tmp < UNIT) {
    return `${tmp.toFixed(1)}K`
  }
  tmp = tmp / UNIT
  if (tmp < UNIT) {
    return `${tmp.toFixed(1)}M`
  }
  tmp = tmp / UNIT
  if (tmp < UNIT) {
    return `${tmp.toFixed(1)}G`
  }
  return `${size}B`
}
