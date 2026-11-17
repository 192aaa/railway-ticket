import { beforeEach, describe, expect, it, vi } from 'vitest'

// vi.mock 工厂会被提升到文件顶部，共享 mock 必须用 vi.hoisted 定义
const mocks = vi.hoisted(() => {
  const store = { token: 'old-token', refresh: 'refresh-1' }
  const handlers = { request: [], response: [] }
  const interceptors = {
    request: { use: vi.fn((...fns) => fns.forEach((f) => handlers.request.push(f))) },
    response: { use: vi.fn((...fns) => fns.forEach((f) => handlers.response.push(f))) }
  }
  const mockAxiosInstance = vi.fn()
  mockAxiosInstance.interceptors = interceptors
  const rawAxiosPost = vi.fn()
  const routerMock = {
    currentRoute: { value: { fullPath: '/home' } },
    replace: vi.fn(() => Promise.resolve())
  }
  const setTokensMock = vi.fn((access, refresh) => {
    store.token = access
    store.refresh = refresh
  })
  const clearTokensMock = vi.fn(() => {
    store.token = ''
    store.refresh = ''
  })
  return { store, handlers, mockAxiosInstance, rawAxiosPost, routerMock, setTokensMock, clearTokensMock }
})

vi.mock('./auth', () => ({
  getToken: () => mocks.store.token,
  getRefreshToken: () => mocks.store.refresh,
  setTokens: mocks.setTokensMock,
  clearTokens: mocks.clearTokensMock
}))

vi.mock('../router', () => ({ default: mocks.routerMock }))

vi.mock('axios', () => {
  const factory = vi.fn(() => mocks.mockAxiosInstance)
  factory.post = mocks.rawAxiosPost
  factory.create = factory
  return { default: factory }
})

import './request'

const reqInterceptor = mocks.handlers.request[0]
const [resSuccess, resError] = mocks.handlers.response

function http401(config = {}) {
  return { response: { status: 401 }, config }
}

describe('request 401 自动刷新与并发队列', () => {
  beforeEach(() => {
    mocks.store.token = 'old-token'
    mocks.store.refresh = 'refresh-1'
    mocks.mockAxiosInstance.mockReset()
    mocks.rawAxiosPost.mockReset()
    mocks.routerMock.replace.mockClear()
    mocks.clearTokensMock.mockClear()
    mocks.setTokensMock.mockClear()
  })

  it('请求拦截器注入当前 access token', () => {
    const config = { headers: {} }
    expect(reqInterceptor(config).headers.Authorization).toBe('Bearer old-token')
  })

  it('业务 code=200 解包 data；非 200 拒绝', async () => {
    expect(resSuccess({ data: { code: 200, data: { id: 1 } } })).toEqual({ id: 1 })
    await expect(resSuccess({ data: { code: 400, message: '参数错误' } })).rejects.toThrow('参数错误')
  })

  it('主动登出的 401 不触发刷新，直接拒绝', async () => {
    await expect(resError(http401({ _skipAuthRefresh: true }))).rejects.toBeDefined()
    expect(mocks.rawAxiosPost).not.toHaveBeenCalled()
    expect(mocks.clearTokensMock).not.toHaveBeenCalled()
  })

  it('无 refresh token 的 401 触发强制登出', async () => {
    mocks.store.refresh = ''
    await expect(resError(http401({ _retry: false }))).rejects.toBeDefined()
    expect(mocks.rawAxiosPost).not.toHaveBeenCalled()
    expect(mocks.clearTokensMock).toHaveBeenCalled()
  })

  it('401 时刷新令牌并用新 token 重放原请求', async () => {
    mocks.rawAxiosPost.mockResolvedValue({
      data: { code: 200, data: { token: 'new-token', refreshToken: 'refresh-2' } }
    })
    mocks.mockAxiosInstance.mockImplementation((config) =>
      Promise.resolve(
        resSuccess({ data: { code: 200, data: { ok: true, auth: config.headers.Authorization } } })
      )
    )

    const result = await resError(http401({ _retry: false, headers: {} }))

    expect(mocks.rawAxiosPost).toHaveBeenCalledTimes(1)
    expect(result).toEqual({ ok: true, auth: 'Bearer new-token' })
    expect(mocks.store.token).toBe('new-token')
  })

  it('两个并发 401 只刷新一次，两个请求都用新 token 重放', async () => {
    let resolveRefresh
    mocks.rawAxiosPost.mockReturnValue(
      new Promise((resolve) => {
        resolveRefresh = resolve
      })
    )
    mocks.mockAxiosInstance.mockImplementation((config) =>
      Promise.resolve(resSuccess({ data: { code: 200, data: { auth: config.headers.Authorization } } }))
    )

    const p1 = resError(http401({ _retry: false, headers: {} }))
    const p2 = resError(http401({ _retry: false, headers: {} }))
    await Promise.resolve()
    await Promise.resolve()

    resolveRefresh({ data: { code: 200, data: { token: 'new-token', refreshToken: 'refresh-2' } } })
    const results = await Promise.all([p1, p2])

    expect(mocks.rawAxiosPost).toHaveBeenCalledTimes(1)
    expect(results).toEqual([{ auth: 'Bearer new-token' }, { auth: 'Bearer new-token' }])
  })

  it('刷新失败时强制登出并拒绝所有等待请求', async () => {
    mocks.rawAxiosPost.mockResolvedValue({ data: { code: 400, message: 'refresh expired' } })

    await expect(resError(http401({ _retry: false, headers: {} }))).rejects.toThrow('refresh expired')
    expect(mocks.clearTokensMock).toHaveBeenCalled()
    expect(mocks.routerMock.replace).toHaveBeenCalled()
  })
})
