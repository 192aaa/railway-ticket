import { test, expect } from '@playwright/test'

const API = '**/api/**'
const LOGIN_USER = 'e2e_tester'

function json(body, status = 200) {
  return { status, contentType: 'application/json', body: JSON.stringify(body) }
}

/** 安装统一 API mock；返回可变状态便于按用例调整 */
async function mockApi(page) {
  const state = {
    loginToken: 'access-initial',
    refreshToken: 'refresh-initial',
    refreshCalled: 0,
    orderCalls: 0,
    firstOrder401: false,
    logoutCalled: 0
  }

  await page.route(API, async (route) => {
    const req = route.request()
    const url = new URL(req.url())
    const path = url.pathname

    if (path.endsWith('/api/user/login')) {
      return route.fulfill(
        json({
          code: 200,
          data: {
            token: state.loginToken,
            refreshToken: state.refreshToken,
            userId: 2,
            username: LOGIN_USER
          },
          message: 'success'
        })
      )
    }
    if (path.endsWith('/api/user/refresh')) {
      state.refreshCalled++
      return route.fulfill(
        json({
          code: 200,
          data: { token: 'access-refreshed', refreshToken: 'refresh-rotated' },
          message: 'success'
        })
      )
    }
    if (path.endsWith('/api/user/logout')) {
      state.logoutCalled++
      return route.fulfill(json({ code: 200, data: null, message: 'success' }))
    }
    if (path.includes('/api/order/my')) {
      state.orderCalls++
      if (state.firstOrder401 && state.orderCalls === 1) {
        return route.fulfill(json({ code: 401, message: 'token expired', data: null }, 401))
      }
      return route.fulfill(
        json({
          code: 200,
          data: [{ id: 1, trainNumber: 'G1', seatNo: '8D', status: 'PAID' }],
          message: 'success'
        })
      )
    }
    // 其余查询类接口返回空列表，避免首页其它请求报错
    return route.fulfill(json({ code: 200, data: [], message: 'success' }))
  })

  return state
}

async function login(page) {
  await page.goto('/login')
  await page.getByPlaceholder('用户名').fill(LOGIN_USER)
  await page.getByPlaceholder('密码').fill('Test@123456')
  await page.getByRole('button', { name: '登 录' }).click()
  await expect(page).toHaveURL(/\/home/)
}

test('登录存储令牌，退出清理会话', async ({ page }) => {
  const state = await mockApi(page)
  await login(page)

  expect(await page.evaluate(() => sessionStorage.getItem('token'))).toBe('access-initial')
  expect(await page.evaluate(() => sessionStorage.getItem('refreshToken'))).toBe('refresh-initial')

  // 退出有二次确认弹窗，确认后才清除会话并跳转
  await page.getByRole('button', { name: '退出', exact: true }).click()
  await page.locator('.el-message-box').getByRole('button', { name: '退出' }).click()

  await expect(page).toHaveURL(/\/login/)
  expect(await page.evaluate(() => sessionStorage.getItem('token'))).toBeNull()
  expect(await page.evaluate(() => sessionStorage.getItem('refreshToken'))).toBeNull()
  expect(state.logoutCalled).toBe(1)
})

test('access token 失效后自动刷新并用新令牌重放请求', async ({ page }) => {
  const state = await mockApi(page)
  state.firstOrder401 = true
  await login(page)

  // 篡改令牌，模拟已过期但仍持有 refresh token
  await page.evaluate(() => sessionStorage.setItem('token', 'access-expired'))

  await page.goto('/orders')

  // 订单页成功渲染说明 401 → refresh → 重放 链路生效
  await expect(page.getByText('G1').first()).toBeVisible({ timeout: 10000 })
  expect(state.refreshCalled).toBe(1)
  expect(state.orderCalls).toBeGreaterThanOrEqual(2)
  expect(await page.evaluate(() => sessionStorage.getItem('token'))).toBe('access-refreshed')
  expect(await page.evaluate(() => sessionStorage.getItem('refreshToken'))).toBe('refresh-rotated')
})
