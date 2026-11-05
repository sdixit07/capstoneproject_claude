import { describe, it, expect } from 'vitest'
import { isNextDisabled, isPrevDisabled, pageLabel, resolvePageResult } from './paginationState'

describe('isPrevDisabled', () => {
  it('is disabled at page 0', () => {
    expect(isPrevDisabled(0, 5, false)).toBe(true)
  })
  it('is enabled at page 1', () => {
    expect(isPrevDisabled(1, 5, false)).toBe(false)
  })
  it('is disabled while loading', () => {
    expect(isPrevDisabled(2, 5, true)).toBe(true)
  })
})

describe('isNextDisabled', () => {
  it('is disabled on the last page', () => {
    expect(isNextDisabled(4, 5, false)).toBe(true)
  })
  it('is disabled when totalPages is 0 or 1', () => {
    expect(isNextDisabled(0, 0, false)).toBe(true)
    expect(isNextDisabled(0, 1, false)).toBe(true)
  })
  it('is enabled before the last page', () => {
    expect(isNextDisabled(0, 5, false)).toBe(false)
    expect(isNextDisabled(3, 5, false)).toBe(false)
  })
  it('is disabled while loading', () => {
    expect(isNextDisabled(0, 5, true)).toBe(true)
  })
})

describe('pageLabel', () => {
  it('formats first and last page', () => {
    expect(pageLabel(0, 5)).toBe('Page 1 of 5')
    expect(pageLabel(4, 5)).toBe('Page 5 of 5')
  })
})

describe('resolvePageResult', () => {
  it('clamps when the page is beyond the last page', () => {
    expect(resolvePageResult(4, 3)).toEqual({ clamp: true, page: 2 })
  })
  it('does not clamp inside the range', () => {
    expect(resolvePageResult(2, 3)).toEqual({ clamp: false, page: 2 })
    expect(resolvePageResult(0, 3)).toEqual({ clamp: false, page: 0 })
  })
  it('clamps to 0 when totalPages is 0 and page is above 0', () => {
    expect(resolvePageResult(2, 0)).toEqual({ clamp: true, page: 0 })
  })
  it('does not clamp page 0 when totalPages is 0', () => {
    expect(resolvePageResult(0, 0)).toEqual({ clamp: false, page: 0 })
  })
})
