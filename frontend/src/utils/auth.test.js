import { beforeEach, describe, expect, it } from 'vitest'
import { USER_KEY, TOKENS_CHANGED, getToken, getRefreshToken, setTokens, clearTokens } from './auth'

describe('auth 令牌存储', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  it('初始状态为空', () => {
    expect(getToken()).toBe('')
    expect(getRefreshToken()).toBe('')
  })

  it('setTokens 写入令牌并派发变更事件', () => {
    let fired = 0
    const onChange = () => fired++
    window.addEventListener(TOKENS_CHANGED, onChange)

    setTokens('access-1', 'refresh-1')

    expect(getToken()).toBe('access-1')
    expect(getRefreshToken()).toBe('refresh-1')
    expect(fired).toBe(1)

    window.removeEventListener(TOKENS_CHANGED, onChange)
  })

  it('setTokens 只传 access 时会清除 refresh（成对写入语义）', () => {
    setTokens('access-1', 'refresh-1')
    setTokens('access-2')

    expect(getToken()).toBe('access-2')
    expect(getRefreshToken()).toBe('')
  })

  it('clearTokens 清空令牌与用户信息并派发事件', () => {
    setTokens('access-1', 'refresh-1')
    sessionStorage.setItem(USER_KEY, JSON.stringify({ id: 1 }))
    let fired = 0
    const onChange = () => fired++
    window.addEventListener(TOKENS_CHANGED, onChange)

    clearTokens()

    expect(getToken()).toBe('')
    expect(getRefreshToken()).toBe('')
    expect(sessionStorage.getItem(USER_KEY)).toBeNull()
    expect(fired).toBe(1)

    window.removeEventListener(TOKENS_CHANGED, onChange)
  })
})
