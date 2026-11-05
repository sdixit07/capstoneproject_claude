export function isPrevDisabled(page, totalPages, disabled) {
  return Boolean(disabled) || page <= 0
}

export function isNextDisabled(page, totalPages, disabled) {
  return Boolean(disabled) || page + 1 >= totalPages
}

export function pageLabel(page, totalPages) {
  return `Page ${page + 1} of ${totalPages}`
}

// Decide what to do with a successful page response (D-8 clamp).
// If the requested page is beyond the last page, ask for the last page instead.
export function resolvePageResult(page, totalPages) {
  const last = Math.max(totalPages - 1, 0)
  if (page > last) {
    return { clamp: true, page: last }
  }
  return { clamp: false, page }
}
