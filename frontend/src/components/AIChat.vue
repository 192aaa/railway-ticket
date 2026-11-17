<template>
  <div class="ai-chat-wrap">
    <Transition name="fade-up">
      <div v-if="open" class="ai-panel glass">
        <div class="ai-header">
          <div class="ai-header-left">
            <div class="ai-avatar">
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M12 2a4 4 0 0 1 4 4v2a4 4 0 0 1-8 0V6a4 4 0 0 1 4-4z" />
                <path d="M16 14h.01" />
                <path d="M8 14h.01" />
                <path d="M12 12v2" />
                <path d="M20 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M4 21v-2a4 4 0 0 1 3-3.87" />
              </svg>
            </div>
            <div>
              <div class="ai-name">RailFlow AI 助手</div>
              <div class="ai-status">
                <span class="dot" :class="{ online: aiConfigured }"></span>
                {{ aiConfigured ? '已连接' : '模拟模式' }}
              </div>
            </div>
          </div>
          <div class="ai-header-actions">
            <button class="action-btn" title="设置" @click="showSettings = true">
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <circle cx="12" cy="12" r="3" />
                <path
                  d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"
                />
              </svg>
            </button>
            <button class="action-btn" title="清空" @click="clearChat">
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M3 6h18" />
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
                <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
            </button>
            <button class="action-btn" title="收起" @click="open = false">
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M18 6 6 18" />
                <path d="m6 6 12 12" />
              </svg>
            </button>
          </div>
        </div>

        <div class="ai-body" ref="bodyRef">
          <div v-if="messages.length === 0" class="ai-welcome">
            <div class="welcome-icon">🚄</div>
            <div class="welcome-title">你好，我是 RailFlow AI</div>
            <div class="welcome-sub">我可以帮你查询车票、推荐路线、解答出行问题</div>
            <div class="quick-prompts">
              <div class="qp-item" @click="sendQuick('北京到上海明天的高铁')">北京→上海 高铁</div>
              <div class="qp-item" @click="sendQuick('推荐一条从广州到成都的线路')">广州→成都 推荐</div>
              <div class="qp-item" @click="sendQuick('高铁和动车有什么区别？')">高铁 vs 动车</div>
              <div class="qp-item" @click="sendQuick('北京出发周末短途去哪好？')">周末短途推荐</div>
            </div>
          </div>

          <div v-for="(m, idx) in messages" :key="idx" class="ai-msg" :class="m.role">
            <div class="msg-avatar" :class="m.role === 'user' ? 'user' : 'ai'">
              {{ m.role === 'user' ? '我' : 'AI' }}
            </div>
            <div class="msg-content">
              <div class="bubble" :class="m.role">
                <span v-html="formatContent(m.content)"></span>
                <span v-if="m.role === 'assistant' && streaming" class="cursor">▊</span>
              </div>
            </div>
          </div>

          <div v-if="loading && !streaming" class="ai-msg assistant">
            <div class="msg-avatar ai">AI</div>
            <div class="msg-content">
              <div class="bubble assistant">
                <div class="typing"><span></span><span></span><span></span></div>
              </div>
            </div>
          </div>
        </div>

        <div class="ai-input">
          <textarea
            v-model="input"
            @keydown.enter.exact.prevent="handleSend"
            placeholder="输入问题，Enter 发送，Shift+Enter 换行"
            :disabled="loading"
            rows="1"
            ref="inputRef"
          ></textarea>
          <button class="send-btn" :disabled="!input.trim() || loading" @click="handleSend">
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="m22 2-7 20-4-9-9-4Z" />
              <path d="M22 2 11 13" />
            </svg>
          </button>
        </div>
      </div>
    </Transition>

    <button v-if="!open" class="ai-fab" @click="open = true">
      <div class="fab-ring"></div>
      <svg
        width="26"
        height="26"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
      <span v-if="unread" class="fab-badge">{{ unread > 99 ? '99+' : unread }}</span>
    </button>

    <div v-if="showSettings" class="modal-overlay" @click.self="showSettings = false">
      <div class="modal-box glass">
        <div class="modal-header">
          <h3>⚙️ AI 设置</h3>
          <button class="modal-close" @click="showSettings = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="setting-row">
            <label>连接状态</label>
            <span class="hint">{{
              aiConfigured ? '已连接服务端大模型' : '模拟模式（服务端未配置密钥）'
            }}</span>
          </div>
          <div class="setting-row">
            <label>当前模型</label>
            <span class="hint">{{ aiModel || '—' }}</span>
          </div>
          <div class="setting-actions">
            <button class="test-btn" @click="testConnection">刷新状态</button>
          </div>
          <div v-if="testResult" class="test-result" :class="testResult.ok ? 'ok' : 'err'">
            {{ testResult.msg }}
          </div>
          <div class="settings-tip">
            🔒 模型密钥由服务端安全持有，不会保存在浏览器中。未配置时自动降级为模拟回复。
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch, computed } from 'vue'
import request from '../utils/request'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

const open = ref(false)
const messages = ref([])
const input = ref('')
const loading = ref(false)
const streaming = ref(false)
const unread = ref(0)
const showSettings = ref(false)
const bodyRef = ref(null)
const inputRef = ref(null)

// 密钥与模型配置全部由服务端持有，浏览器只保存会话级状态
const aiConfigured = ref(false)
const aiModel = ref('')
const testResult = ref(null)

const CHAT_KEY = 'railflow_ai_chat'

async function loadAiStatus() {
  try {
    const status = await request.get('/ai/status')
    aiConfigured.value = !!status.configured
    aiModel.value = status.model || ''
  } catch {
    aiConfigured.value = false
  }
}

async function testConnection() {
  await loadAiStatus()
  testResult.value = aiConfigured.value
    ? { ok: true, msg: `✅ 服务端已配置模型 ${aiModel.value}，密钥由服务端安全持有` }
    : { ok: false, msg: '当前为模拟模式：服务端未配置 AI_API_KEY，可正常体验基础对话' }
}

function scrollToBottom() {
  nextTick(() => {
    if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  })
}

function formatContent(text) {
  if (!text) return ''
  let s = text
    .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
  return s
}

async function fetchTrains(from, to, date) {
  try {
    const params = new URLSearchParams()
    if (from) params.set('fromStation', from)
    if (to) params.set('toStation', to)
    if (date) params.set('date', date)
    const data = await request.get('/ticket/query?' + params.toString())
    return data
  } catch {
    return null
  }
}

function extractStation(text) {
  const m = text.match(/([\u4e00-\u9fa5]{2,6})(?:到|去|至)?[\u4e00-\u9fa5]{0,2}(?:高铁|动车|火车|列车)?/)
  return m ? m[1] : null
}

function mockReply(userText) {
  const t = userText.toLowerCase()
  let response = ''

  if (t.includes('高铁') && t.includes('动车')) {
    response = `🚄 **高铁 vs 动车** 简单说：

- **高铁 (G/C 字头)**：设计时速 350km/h，最快的那种，走专用无砟轨道
- **动车 (D 字头)**：设计时速 250km/h，可以走高铁轨道也可以走普通铁路

票价：高铁比动车贵 20%-40% 左右。

想查具体车次，告诉我 **出发站 → 到达站** 就行！`
  } else if (t.includes('推荐') || t.includes('去哪') || t.includes('短途')) {
    response = `🗺️ **周末短途推荐**（假设从北京出发）：

1. **天津** — 30 分钟，看海逛吃，推荐 Z1 次直达
2. **济南** — 2 小时，大明湖趵突泉，G207 很方便
3. **太原** — 2.5 小时，晋祠平遥古城
4. **秦皇岛** — 2 小时，北戴河看海

告诉我你的出发地和日期，我帮你查具体车票！`
  } else if (/查|票|车|几列|有没有|余票|时刻/.test(userText)) {
    response = `🔍 好的，我来帮你查票！请告诉我：

- 出发站（如"北京"）
- 到达站（如"上海"）
- 日期（如"明天"或"2026-10-20"）

你可以直接说 "北京到上海明天的高铁"，我会自动解析！`
  } else if (t.includes('你好') || t.includes('hi') || t.includes('在吗')) {
    response = `你好！👋 我是 RailFlow AI 助手，可以帮你：

- 🚄 查询车次余票
- 🗺️ 推荐出行路线
- 💡 解答购票疑问

试试问我 "北京到上海明天的高铁"！`
  } else if (t.includes('退款') || t.includes('退')) {
    response = `💰 **退票规则**：

- 发车前 **8 天以上**：免费
- 发车前 **2-7 天**：扣 5%
- 发车前 **24 小时-2 天**：扣 10%
- 发车前 **24 小时内**：扣 20%

在【我的订单】里可以直接申请退票。`
  } else {
    response = `收到！我来帮你看看 👀

不过我现在没连接 AI 模型（模拟模式），暂时只能处理常见问题。

如需完整 AI 能力，请让服务端配置 AI_API_KEY 环境变量；密钥由服务端安全持有。

你可以试试问我：
- "北京到上海明天的高铁"
- "高铁和动车有什么区别"
- "周末短途去哪好"`
  }

  return response
}

const SYSTEM_PROMPT = `你是 RailFlow 火车票系统的 AI 助手，擅长铁路出行规划。你拥有以下工具：

## 你的工具
1. **query_tickets** - 查真实余票
2. **buy_ticket** - 购买火车票（必须先调用 query_tickets 拿到 trainId）

## 购票流程（务必按顺序）
当用户说"帮我买"、"订票"、"下单"等：
1. 先调用 query_tickets 查票
2. 从结果里选最合适的车次，提取 trainId
3. 问用户确认："为您选择了 G7 次 08:00-12:30 北京→上海，二等座 ¥553，可以吗？乘车人姓名和证件号是？"
4. 用户确认并提供乘车人信息后，再调用 buy_ticket
5. 把订单号、车次、时间、价格用🎉庆祝语气告诉用户

## 注意事项
- buy_ticket 需要用户**登录后**才能用，未登录时要引导用户去登录
- seatType 参数可选值："商务座"、"一等座"、"二等座"、"软卧"、"硬卧"、"硬座"
- 永远先问清楚乘车人姓名和证件号，不要瞎猜
- 如果用户说"最便宜的"、"最快的"，你自己从查票结果里选

## 查票工具的要求
- query_tickets 三个参数：fromStation（出发站）、toStation（到达站）、date（YYYY-MM-DD）
- "明天"、"周五"等相对日期要先算成具体日期
- 没说日期默认今天

## 回答风格
- 简洁友好，多用 emoji
- 查票结果用表格，总结推荐
- 购票成功用🎉🌟庆祝一下`

const TOOLS = [
  {
    type: 'function',
    function: {
      name: 'query_tickets',
      description: '查询指定日期两站之间的火车余票、车次时刻、座位价格',
      parameters: {
        type: 'object',
        properties: {
          fromStation: { type: 'string', description: '出发站名，如"北京"、"上海虹桥"' },
          toStation: { type: 'string', description: '到达站名，如"上海"、"广州南"' },
          date: { type: 'string', description: '出行日期，YYYY-MM-DD，如 "2026-10-20"' }
        },
        required: ['fromStation', 'toStation', 'date']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'buy_ticket',
      description:
        '购买火车票，基于 trainId 下单。调用前必须先调用 query_tickets 查票获取 trainId，并已经获得用户确认。',
      parameters: {
        type: 'object',
        properties: {
          trainId: { type: 'number', description: '车次ID，从 query_tickets 返回的数据中获取' },
          trainNumber: { type: 'string', description: '车次号，仅用于记录' },
          trainDate: { type: 'string', description: '出行日期 YYYY-MM-DD' },
          seatType: { type: 'string', description: '座位类型：商务座/一等座/二等座/软卧/硬卧/硬座' },
          passengerName: { type: 'string', description: '乘车人姓名' },
          passengerIdCard: { type: 'string', description: '乘车人证件号' }
        },
        required: ['trainId', 'trainDate', 'seatType', 'passengerName', 'passengerIdCard']
      }
    }
  }
]

function resolveRelativeDate(text) {
  const now = new Date()
  const match = text.match(/(?:明天|后天|大后天|今天|后天|(\d+)天之后)/)
  if (match) {
    const offset = match[1]
      ? parseInt(match[1])
      : match[0] === '今天'
        ? 0
        : match[0] === '明天'
          ? 1
          : match[0] === '后天'
            ? 2
            : match[0] === '大后天'
              ? 3
              : parseInt(match[1]) || 0
    const d = new Date(now)
    d.setDate(d.getDate() + offset)
    return d.toISOString().slice(0, 10)
  }
  const weekMap = { 周一: 1, 周二: 2, 周三: 3, 周四: 4, 周五: 5, 周六: 6, 周日: 0, 星期日: 0 }
  for (const [cn, dow] of Object.entries(weekMap)) {
    if (text.includes(cn)) {
      const d = new Date(now)
      const cur = d.getDay()
      let diff = dow - cur
      if (diff <= 0) diff += 7
      d.setDate(d.getDate() + diff)
      return d.toISOString().slice(0, 10)
    }
  }
  const iso = text.match(/(\d{4}-\d{1,2}-\d{1,2})/)
  if (iso) return iso[1].replace(/(\d{1,2})/g, (m) => m.padStart(2, '0'))
  return now.toISOString().slice(0, 10)
}

async function runTool(name, args) {
  if (name === 'query_tickets') {
    const { fromStation, toStation, date } = args
    try {
      const data = await fetchTrains(fromStation, toStation, date)
      if (!data || data.length === 0) {
        return `查询结果为空：${fromStation} → ${toStation} (${date}) 没有找到车次。请检查站名是否正确。`
      }
      const summary = data
        .slice(0, 15)
        .map((t) => {
          const seats = []
          if (t.businessSeatCount > 0) seats.push(`商务 ${t.businessSeatCount}张 ¥${t.businessSeatPrice}`)
          if (t.firstSeatCount > 0) seats.push(`一等 ${t.firstSeatCount}张 ¥${t.firstSeatPrice}`)
          if (t.secondSeatCount > 0) seats.push(`二等 ${t.secondSeatCount}张 ¥${t.secondSeatPrice}`)
          if (t.sleeperCount > 0) seats.push(`卧铺 ${t.sleeperCount}张 ¥${t.sleeperPrice}`)
          return `| ${t.trainNumber} | ${t.trainType} | id=${t.trainId} | ${t.startTime}-${t.endTime} | ${t.duration}分钟 | ${seats.join(' / ') || '无票'} |`
        })
        .join('\n')
      return `查询成功！${fromStation} → ${toStation} (${date})，共 ${data.length} 车次：\n\n| 车次 | 车型 | trainId | 时间 | 历时 | 余票/价格 |\n|---|---|---|---|---|---|\n${summary}\n\n请根据用户需求（最快/最便宜/推荐）挑选合适车次，记得把 trainId 告诉我以便购票时使用。`
    } catch (e) {
      return `查询失败: ${e.message}`
    }
  }
  if (name === 'buy_ticket') {
    if (!isLoggedIn.value) {
      return `ERROR_NOT_LOGGED_IN: 用户未登录，无法购票。请引导用户先登录。`
    }
    const { trainId, trainDate, seatType, passengerName, passengerIdCard } = args
    if (!trainId || !trainDate || !seatType || !passengerName || !passengerIdCard) {
      return `ERROR_MISSING_PARAMS: 购票参数不完整，还需要用户确认乘车人信息。`
    }
    try {
      const order = await request.post('/order/buy', {
        trainId,
        trainDate,
        seatType,
        passengerName,
        passengerIdCard
      })
      return `购票成功！\n- 订单号: ${order.id}\n- 车次: ${order.trainNumber || args.trainNumber}\n- 日期: ${order.trainDate || trainDate}\n- 座位: ${seatType}\n- 乘车人: ${passengerName}\n请用🎉语气告诉用户。`
    } catch (e) {
      return `ERROR_BUY_FAILED: ${e.message}`
    }
  }
  return `未知工具: ${name}`
}

async function callAI(messages, tools = null) {
  // 只把对话内容发给后端代理；密钥、模型、温度均由服务端配置，浏览器不接触。
  const body = {
    messages: [
      { role: 'system', content: SYSTEM_PROMPT },
      ...messages.map((m) => ({
        role: m.role,
        content: m.content,
        tool_calls: m.tool_calls || undefined,
        tool_call_id: m.tool_call_id || undefined
      }))
    ],
    ...(tools ? { tools } : {})
  }
  // request 响应拦截器已解包 Result.data，这里拿到的是上游原始响应对象
  return request.post('/ai/chat', body)
}

async function callAIWithTools(userText, existingHistory) {
  let messages = [...existingHistory, { role: 'user', content: userText }]

  for (let round = 0; round < 3; round++) {
    const data = await callAI(messages, TOOLS)
    const msg = data.choices?.[0]?.message
    if (!msg) return '抱歉，AI 没有返回内容'

    if (msg.tool_calls && msg.tool_calls.length > 0) {
      messages.push({
        role: 'assistant',
        content: msg.content || '',
        tool_calls: msg.tool_calls
      })
      for (const tc of msg.tool_calls) {
        const args = JSON.parse(tc.function.arguments || '{}')
        const toolResult = await runTool(tc.function.name, args)
        messages.push({
          role: 'tool',
          tool_call_id: tc.id,
          content: toolResult
        })
      }
      continue
    }

    return msg.content || '抱歉，我没理解你的问题'
  }

  return 'AI 调用轮数过多，请简化你的问题再试'
}

let pendingBuy = null

async function smartMockReply(userText) {
  const t = userText

  if (/买|订|购票|下单/.test(t) && /到|去|至|->|→/.test(t)) {
    if (!isLoggedIn.value) {
      return `🔐 购票需要先登录哦～点击页面右上角登录/注册吧！登录后我就可以帮你买票了 ✨`
    }
    const date = resolveRelativeDate(t)
    const m = t.match(/([\u4e00-\u9fa5A-Za-z]{2,8})\s*(?:到|去|至|->|→|)\s*([\u4e00-\u9fa5A-Za-z]{2,8})/)
    if (m) {
      const from = m[1],
        to = m[2]
      try {
        const data = await fetchTrains(from, to, date)
        if (!data || data.length === 0) return `🔍 ${from} → ${to} (${date}) 没有车次`
        const available = data.filter(
          (tr) =>
            tr.secondSeatCount > 0 || tr.firstSeatCount > 0 || tr.sleeperCount > 0 || tr.businessSeatCount > 0
        )
        if (available.length === 0) return `😢 ${from} → ${to} (${date}) 全部无票了`
        const chosen = available[0]
        let seatType = '二等座'
        let seatPrice = chosen.secondSeatPrice
        if (chosen.secondSeatCount <= 0 && chosen.firstSeatCount > 0) {
          seatType = '一等座'
          seatPrice = chosen.firstSeatPrice
        } else if (chosen.firstSeatCount <= 0 && chosen.businessSeatCount > 0) {
          seatType = '商务座'
          seatPrice = chosen.businessSeatPrice
        } else if (chosen.sleeperCount > 0 && chosen.secondSeatCount <= 0) {
          seatType = '硬卧'
          seatPrice = chosen.sleeperPrice
        }

        pendingBuy = {
          trainId: chosen.trainId,
          trainNumber: chosen.trainNumber,
          trainDate: date,
          seatType,
          seatPrice,
          from,
          to,
          passengerName: null,
          passengerIdCard: null
        }

        let out = `🚄 我帮你查好啦！推荐 **${chosen.trainNumber}** 次列车\n`
        out += `${from} ${chosen.startTime} → ${to} ${chosen.endTime} (${chosen.duration}分钟)\n\n`
        out += `🎫 选 **${seatType}** ¥${seatPrice}\n\n`
        out += `请告诉我乘车人的**姓名**和**身份证号**，我直接帮你下单！\n`
        out += `比如："张三 110101199001011234"`
        return out
      } catch {
        return mockReply(userText)
      }
    }
  }

  if (pendingBuy && !pendingBuy.passengerName) {
    const idCardMatch = t.match(/(\d{15}|\d{17}[\dXx])/)
    const nameMatch = t.match(/^([\u4e00-\u9fa5]{2,6})/)
    if (idCardMatch) pendingBuy.passengerIdCard = idCardMatch[1]
    if (nameMatch && !/^\d/.test(t)) pendingBuy.passengerName = nameMatch[1]

    if (pendingBuy.passengerName && pendingBuy.passengerIdCard) {
      try {
        const order = await request.post('/order/buy', {
          trainId: pendingBuy.trainId,
          trainDate: pendingBuy.trainDate,
          seatType: pendingBuy.seatType,
          passengerName: pendingBuy.passengerName,
          passengerIdCard: pendingBuy.passengerIdCard
        })
        const from = pendingBuy.from,
          to = pendingBuy.to
        let out = `🎉🎉🎉 购票成功！\n\n`
        out += `**订单号 #${order.id}**\n`
        out += `🚄 ${order.trainNumber || pendingBuy.trainNumber} ${from} → ${to}\n`
        out += `📅 ${pendingBuy.trainDate} ${order.startTime || ''}→${order.endTime || ''}\n`
        out += `💺 ${pendingBuy.seatType}\n`
        out += `👤 ${pendingBuy.passengerName}\n\n`
        out += `✨ 祝你旅途愉快！可以在右上角「我的订单」查看详情～`
        pendingBuy = null
        return out
      } catch (e) {
        pendingBuy = null
        return `❌ 购票失败: ${e.message}\n你可以在首页手动购票试试。`
      }
    }
    let out = '好的，还需要你的：\n'
    if (!pendingBuy.passengerName) out += '- 乘车人**姓名**（2-6个汉字）\n'
    if (!pendingBuy.passengerIdCard) out += '- **身份证号**\n'
    out += '\n一起发给我就行～'
    return out
  }

  if (/查|票|车|余票|有没有|几列|时刻|时刻表|票价/.test(t) && /到|去|至|->|→/.test(t)) {
    const date = resolveRelativeDate(t)
    const m = t.match(/([\u4e00-\u9fa5A-Za-z]{2,8})\s*(?:到|去|至|->|→|)\s*([\u4e00-\u9fa5A-Za-z]{2,8})/)
    if (m) {
      const from = m[1]
      const to = m[2]
      try {
        const data = await fetchTrains(from, to, date)
        if (data && data.length > 0) {
          let out = `🚄 **${from} → ${to}** (${date})\n\n`
          out += `共找到 **${data.length}** 个车次：\n\n`
          data.slice(0, 10).forEach((train, i) => {
            const seats = []
            if (train.businessSeatCount > 0) seats.push(`商务¥${train.businessSeatPrice}`)
            if (train.firstSeatCount > 0) seats.push(`一等¥${train.firstSeatPrice}`)
            if (train.secondSeatCount > 0) seats.push(`二等¥${train.secondSeatPrice}`)
            if (train.sleeperCount > 0) seats.push(`卧铺¥${train.sleeperPrice}`)
            out += `${i + 1}. **${train.trainNumber}** ${train.startTime}→${train.endTime} (${train.duration}分钟) — ${seats.join(' / ') || '无票'}\n`
          })
          out += `\n💡 想要更智能的推荐？让服务端配置 AI_API_KEY 即可解锁完整能力！`
          return out
        }
        return `🔍 ${from} → ${to} (${date}) 没有找到车次。试试其他日期或检查站名？`
      } catch {
        return mockReply(userText)
      }
    }
  }
  return mockReply(userText)
}

async function handleSend() {
  const text = input.value.trim()
  if (!text || loading.value) return
  input.value = ''

  messages.value.push({ role: 'user', content: text })
  loading.value = true
  scrollToBottom()

  try {
    let reply
    if (aiConfigured.value) {
      const history = messages.value.slice(-10)
      reply = await callAIWithTools(text, history)
    } else {
      await new Promise((r) => setTimeout(r, 400 + Math.random() * 400))
      reply = await smartMockReply(text)
    }
    messages.value.push({ role: 'assistant', content: reply })
  } catch (e) {
    messages.value.push({
      role: 'assistant',
      content: `❌ 调用失败: ${e.message}\n\n请检查 API Key 和网络连接。`
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function sendQuick(text) {
  input.value = text
  handleSend()
}

function clearChat() {
  messages.value = []
  pendingBuy = null
  localStorage.removeItem(CHAT_KEY)
}

watch(open, (v) => {
  if (v) {
    unread.value = 0
    nextTick(() => inputRef.value?.focus())
  }
})

onMounted(() => {
  loadAiStatus()
  try {
    const raw = localStorage.getItem(CHAT_KEY)
    if (raw) messages.value = JSON.parse(raw)
  } catch {}
})

watch(
  messages,
  () => {
    localStorage.setItem(CHAT_KEY, JSON.stringify(messages.value.slice(-50)))
  },
  { deep: true }
)
</script>

<style scoped>
.ai-chat-wrap {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 9998;
}

.ai-panel {
  width: 400px;
  height: 560px;
  max-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.95), rgba(30, 41, 59, 0.92));
  border: 1px solid rgba(96, 165, 250, 0.25);
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(96, 165, 250, 0.1);
}

.fade-up-enter-active,
.fade-up-leave-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.fade-up-enter-from,
.fade-up-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

.ai-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  background: rgba(15, 23, 42, 0.5);
}

.ai-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
}

.ai-name {
  font-size: 15px;
  font-weight: 700;
  color: #e2e8f0;
}

.ai-status {
  font-size: 11px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 5px;
}

.ai-status .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #64748b;
}
.ai-status .dot.online {
  background: #22c55e;
  box-shadow: 0 0 8px #22c55e;
}

.ai-header-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.action-btn:hover {
  background: rgba(96, 165, 250, 0.2);
  color: #60a5fa;
}

.ai-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ai-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 8px;
  text-align: center;
  padding: 20px;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 4px;
}

.welcome-title {
  font-size: 18px;
  font-weight: 700;
  color: #e2e8f0;
}

.welcome-sub {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 16px;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.qp-item {
  padding: 8px 14px;
  border-radius: 20px;
  border: 1px solid rgba(96, 165, 250, 0.25);
  background: rgba(59, 130, 246, 0.1);
  color: #93c5fd;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.qp-item:hover {
  background: rgba(59, 130, 246, 0.25);
  border-color: rgba(96, 165, 250, 0.5);
  transform: translateY(-1px);
}

.ai-msg {
  display: flex;
  gap: 10px;
  animation: msgIn 0.3s ease-out;
}

@keyframes msgIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-msg.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: white;
}
.msg-avatar.user {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
}
.msg-avatar.ai {
  background: linear-gradient(135deg, #10b981, #06b6d4);
}

.msg-content {
  max-width: 75%;
}

.bubble {
  padding: 11px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}
.bubble.user {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
}
.bubble.assistant {
  background: rgba(30, 41, 59, 0.85);
  color: #e2e8f0;
  border-bottom-left-radius: 4px;
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.bubble :deep(code) {
  background: rgba(0, 0, 0, 0.3);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.bubble :deep(pre) {
  background: rgba(0, 0, 0, 0.3);
  padding: 10px;
  border-radius: 8px;
  overflow-x: auto;
}

.cursor {
  animation: blink 0.8s infinite;
}
@keyframes blink {
  0%,
  50% {
    opacity: 1;
  }
  51%,
  100% {
    opacity: 0;
  }
}

.typing {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.typing span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #64748b;
  animation: bounce 1.4s infinite;
}
.typing span:nth-child(2) {
  animation-delay: 0.2s;
}
.typing span:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-6px);
  }
}

.ai-input {
  padding: 12px 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
  display: flex;
  gap: 10px;
  align-items: flex-end;
  background: rgba(15, 23, 42, 0.5);
}

.ai-input textarea {
  flex: 1;
  resize: none;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(15, 23, 42, 0.6);
  color: #e2e8f0;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  outline: none;
  max-height: 120px;
  font-family: inherit;
  line-height: 1.5;
  transition: all 0.2s;
}
.ai-input textarea:focus {
  border-color: rgba(96, 165, 250, 0.5);
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.15);
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
  flex-shrink: 0;
}
.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.55);
}
.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-fab {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 30px rgba(59, 130, 246, 0.5);
  transition: all 0.3s;
}
.ai-fab:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 12px 40px rgba(59, 130, 246, 0.7);
}
.ai-fab:active {
  transform: scale(0.95);
}

.fab-ring {
  position: absolute;
  inset: -3px;
  border-radius: 20px;
  border: 2px solid rgba(96, 165, 250, 0.5);
  animation: ring 2s ease-out infinite;
  pointer-events: none;
}
@keyframes ring {
  0% {
    transform: scale(1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1.3);
    opacity: 0;
  }
}

.fab-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #ef4444;
  color: white;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #1e293b;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  animation: fadeIn 0.2s;
}
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-box {
  width: 420px;
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.97), rgba(30, 41, 59, 0.95));
  border: 1px solid rgba(96, 165, 250, 0.25);
  animation: popIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes popIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(16px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}
.modal-header h3 {
  margin: 0;
  font-size: 15px;
  color: #e2e8f0;
  font-weight: 700;
}
.modal-close {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: none;
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}
.modal-close:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #fca5a5;
}

.modal-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.setting-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.setting-row label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}
.setting-row input[type='text'],
.setting-row input[type='password'] {
  width: 100%;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(15, 23, 42, 0.6);
  color: #e2e8f0;
  font-size: 13px;
  outline: none;
  box-sizing: border-box;
  transition: all 0.2s;
}
.setting-row input:focus {
  border-color: rgba(96, 165, 250, 0.5);
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.15);
}
.setting-row .hint {
  font-size: 11px;
  color: #64748b;
}
.setting-row input[type='range'] {
  width: 100%;
  accent-color: #3b82f6;
}

.setting-actions {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}
.save-btn,
.test-btn {
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  border: none;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.save-btn {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
}
.save-btn:hover {
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
}
.test-btn {
  background: rgba(148, 163, 184, 0.12);
  color: #94a3b8;
}
.test-btn:hover {
  background: rgba(148, 163, 184, 0.25);
  color: #e2e8f0;
}

.test-result {
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 12px;
}
.test-result.ok {
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.25);
  color: #4ade80;
}
.test-result.err {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.25);
  color: #fca5a5;
}

.settings-tip {
  font-size: 11px;
  color: #64748b;
  padding: 10px;
  background: rgba(96, 165, 250, 0.06);
  border-radius: 8px;
}

@media (max-width: 480px) {
  .ai-chat-wrap {
    right: 12px;
    bottom: 12px;
  }
  .ai-panel {
    width: calc(100vw - 24px);
    height: calc(100vh - 100px);
    max-height: none;
  }
}
</style>
