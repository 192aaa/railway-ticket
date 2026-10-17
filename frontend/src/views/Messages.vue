<template>
  <div class="messages-page">
    <div class="msg-container glass">
      <div class="sidebar">
        <div class="sidebar-header">
          <h2 class="sidebar-title">💬 消息中心</h2>
          <div class="header-right">
            <button class="new-msg-btn" @click="showNewMsgModal = true" title="发新消息">✏️</button>
            <span v-if="unreadCount > 0" class="unread-total">
              {{ unreadCount > 99 ? '99+' : unreadCount }}
            </span>
          </div>
        </div>

        <div class="search-box">
          <input v-model="searchKw" placeholder="🔍 搜索会话..." class="search-input" />
        </div>

        <div class="conv-list">
          <div
            v-for="conv in filteredConvs"
            :key="conv.conversationId"
            class="conv-item stagger-child"
            :class="{ active: activeConvId === conv.conversationId, system: conv.isSystem }"
            @click="openConv(conv)"
          >
            <div class="conv-avatar" :class="{ system: conv.isSystem }">
              {{ conv.avatar }}
            </div>
            <div class="conv-info">
              <div class="conv-top">
                <span class="conv-name">{{ conv.name }}</span>
                <span class="conv-time">{{ formatShortTime(conv.latestTime) }}</span>
              </div>
              <div class="conv-bottom">
                <span class="conv-preview">{{ truncate(conv.latestMessage, 30) }}</span>
                <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount }}</span>
              </div>
            </div>
          </div>

          <div v-if="conversations.length === 0" class="empty-state">
            <div class="empty-icon">📭</div>
            <p>暂无消息</p>
          </div>
        </div>
      </div>

      <div class="chat-area">
        <template v-if="activeConv">
          <div class="chat-header">
            <div class="chat-header-left">
              <div class="chat-avatar" :class="{ system: activeConv.isSystem }">{{ activeConv.avatar }}</div>
              <div>
                <div class="chat-name">{{ activeConv.name }}</div>
                <div class="chat-status">在线</div>
              </div>
            </div>
          </div>

          <div class="chat-body" ref="chatBodyRef">
            <div v-if="chatHistory.length === 0" class="empty-chat">
              <div class="empty-icon">💭</div>
              <p>暂无聊天记录</p>
            </div>

            <div
              v-for="(msg, idx) in chatHistory"
              :key="msg.id || idx"
              class="msg-bubble"
              :class="{ mine: msg.senderId === myUserId, system: msg.type === 'SYSTEM' }"
            >
              <template v-if="msg.type === 'SYSTEM'">
                <div class="sys-bubble">
                  <span class="sys-icon">🔔</span>
                  <span>{{ msg.content }}</span>
                </div>
              </template>
              <template v-else>
                <div v-if="msg.senderId !== myUserId" class="msg-avatar other-avatar">
                  {{ (activeConv?.name || 'U').substring(0, 1).toUpperCase() }}
                </div>
                <div class="bubble-wrap">
                  <div v-if="msg.senderId !== myUserId" class="bubble-sender-name">{{ activeConv?.name || '对方' }}</div>
                  <div class="bubble-text">{{ msg.content }}</div>
                  <div class="bubble-time">{{ formatShortTime(msg.createTime) }}</div>
                </div>
                <div v-if="msg.senderId === myUserId" class="msg-avatar mine-avatar">
                  {{ (userStore.username || 'M').substring(0, 1).toUpperCase() }}
                </div>
              </template>
            </div>
          </div>

          <div class="chat-input-area">
            <input
              v-model="inputMsg"
              @keyup.enter="sendMsg"
              :placeholder="activeConv.isSystem ? '系统通知不可回复' : '输入消息...'"
              class="chat-input"
              :disabled="activeConv.isSystem"
            />
            <button
              class="send-btn"
              :disabled="!inputMsg.trim() || activeConv.isSystem || sending"
              @click="sendMsg"
            >
              {{ sending ? '发送中...' : '发送' }}
            </button>
          </div>
        </template>

        <div v-else class="no-chat-selected">
          <div class="empty-icon">👈</div>
          <p>从左侧选择一个会话开始聊天</p>
        </div>
      </div>
    </div>

    <div v-if="showNewMsgModal" class="modal-overlay" @click.self="showNewMsgModal = false">
      <div class="modal-box glass">
        <div class="modal-header">
          <h3>✏️ 发起新对话</h3>
          <button class="modal-close" @click="showNewMsgModal = false">✕</button>
        </div>
        <div class="modal-body">
          <input
            v-model="searchUserKw"
            @input="debounceSearchUsers"
            placeholder="🔍 输入用户名或姓名..."
            class="user-search-input"
          />
          <div class="user-search-results">
            <div v-if="searchUserLoading" class="search-tip">搜索中...</div>
            <div v-else-if="searchedUsers.length === 0 && searchUserKw.trim()" class="search-tip">未找到用户</div>
            <template v-else>
              <div
                v-for="u in searchedUsers"
                :key="u.id"
                class="user-item"
                @click="selectUser(u)"
              >
                <div class="user-item-avatar">{{ u.username.substring(0, 1).toUpperCase() }}</div>
                <div class="user-item-info">
                  <div class="user-item-name">{{ u.username }} <span v-if="u.realName" class="real-name">({{ u.realName }})</span></div>
                  <div class="user-item-phone" v-if="u.phoneMasked">{{ u.phoneMasked }}</div>
                </div>
                <button class="start-chat-btn">💬 发消息</button>
              </div>
              <div v-if="!searchUserKw.trim()" class="search-tip">输入关键词开始搜索</div>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import request from '../utils/request'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const myUserId = computed(() => userStore.userInfo?.id || userStore.userInfo?.userId)

const conversations = ref([])
const activeConv = ref(null)
const activeConvId = ref(null)
const chatHistory = ref([])
const inputMsg = ref('')
const sending = ref(false)
const searchKw = ref('')
const unreadCount = ref(0)
const chatBodyRef = ref(null)
const showNewMsgModal = ref(false)
const searchUserKw = ref('')
const searchedUsers = ref([])
const searchUserLoading = ref(false)

let pollTimer = null
let searchTimer = null

const filteredConvs = computed(() => {
  if (!searchKw.value.trim()) return conversations.value
  return conversations.value.filter(c =>
    c.name.toLowerCase().includes(searchKw.value.toLowerCase()) ||
    c.latestMessage.toLowerCase().includes(searchKw.value.toLowerCase())
  )
})

function formatShortTime(iso) {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    const now = new Date()
    const diff = now - d
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
    if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
    if (diff < 7 * 86400000) return Math.floor(diff / 86400000) + ' 天前'
    return iso.slice(0, 10)
  } catch {
    return iso
  }
}

function truncate(str, n) {
  if (!str) return ''
  return str.length > n ? str.slice(0, n) + '...' : str
}

async function loadConversations() {
  try {
    conversations.value = await request.get('/message/conversations')
  } catch (e) {
    console.error('加载会话失败', e)
  }
}

async function loadUnreadCount() {
  try {
    unreadCount.value = await request.get('/message/unread-count')
  } catch {}
}

async function openConv(conv) {
  activeConv.value = conv
  activeConvId.value = conv.conversationId
  try {
    const data = await request.get('/message/history', { params: { userId2: conv.conversationId } })
    chatHistory.value = data || []
    await request.post('/message/read', null, { params: { conversationUserId: conv.conversationId } })
    await loadConversations()
    await loadUnreadCount()
    nextTick(() => scrollToBottom())
  } catch (e) {
    console.error('加载聊天记录失败', e)
  }
}

async function sendMsg() {
  if (!inputMsg.value.trim() || sending.value || activeConv.value?.isSystem) return
  sending.value = true
  const text = inputMsg.value.trim()
  inputMsg.value = ''
  try {
    await request.post('/message/send', { receiverId: activeConv.value.conversationId, content: text })
    await openConv(activeConv.value)
  } catch {}
  sending.value = false
}

function scrollToBottom() {
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}

async function doSearchUsers() {
  const kw = searchUserKw.value.trim()
  if (!kw) { searchedUsers.value = []; return }
  searchUserLoading.value = true
  try {
    searchedUsers.value = await request.get('/message/search-users', { params: { kw } })
  } catch {}
  searchUserLoading.value = false
}

function debounceSearchUsers() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(doSearchUsers, 300)
}

function selectUser(u) {
  const fakeConv = {
    conversationId: u.id,
    isSystem: false,
    name: u.username,
    avatar: u.username.substring(0, 1).toUpperCase(),
    latestMessage: '',
    latestTime: '',
    unreadCount: 0
  }
  showNewMsgModal.value = false
  searchUserKw.value = ''
  searchedUsers.value = []
  openConv(fakeConv)
}

onMounted(async () => {
  await Promise.all([loadConversations(), loadUnreadCount()])
  pollTimer = setInterval(async () => {
    await loadUnreadCount()
    if (activeConv.value) {
      try {
        const data = await request.get('/message/history', { params: { userId2: activeConv.value.conversationId } })
        const newLen = data?.length || 0
        const oldLen = chatHistory.value.length
        chatHistory.value = data || []
        if (newLen > oldLen) {
          await request.post('/message/read', null, { params: { conversationUserId: activeConv.value.conversationId } })
          await loadConversations()
          nextTick(() => scrollToBottom())
        }
      } catch {}
    } else {
      await loadConversations()
    }
  }, 3000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

watch(conversations, () => {
  if (activeConvId.value != null) {
    const fresh = conversations.value.find(c => c.conversationId === activeConvId.value)
    if (fresh) activeConv.value = fresh
  }
}, { deep: true })
</script>

<style scoped>
.messages-page {
  min-height: calc(100vh - 120px);
  padding: 24px;
  display: flex;
  justify-content: center;
  animation: fadeUp 0.4s ease-out;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg-container {
  width: 100%;
  max-width: 1100px;
  min-height: calc(100vh - 168px);
  display: flex;
  border-radius: 20px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.85), rgba(30, 41, 59, 0.7));
  border: 1px solid rgba(148, 163, 184, 0.15);
}

.sidebar {
  width: 340px;
  flex-shrink: 0;
  border-right: 1px solid rgba(148, 163, 184, 0.12);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 22px 20px 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.sidebar-title {
  font-size: 20px;
  font-weight: 700;
  color: #e2e8f0;
  background: linear-gradient(90deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.unread-total {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ef4444, #f97316);
  color: white;
  font-weight: 600;
  animation: pulseRed 2s ease-in-out infinite;
}

@keyframes pulseRed {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(239, 68, 68, 0); }
}

.search-box {
  padding: 12px 16px;
}

.search-input {
  width: 100%;
  height: 38px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.15);
  background: rgba(15, 23, 42, 0.6);
  color: #e2e8f0;
  font-size: 13px;
  outline: none;
  transition: all 0.3s;
}

.search-input:focus {
  border-color: rgba(96, 165, 250, 0.5);
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.15);
}

.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s;
  margin-bottom: 4px;
  border: 1px solid transparent;
}

.conv-item:hover {
  background: rgba(96, 165, 250, 0.08);
  border-color: rgba(96, 165, 250, 0.2);
}

.conv-item.active {
  background: rgba(96, 165, 250, 0.12);
  border-color: rgba(96, 165, 250, 0.35);
}

.conv-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: white;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  flex-shrink: 0;
}

.conv-avatar.system {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  font-size: 22px;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-top, .conv-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.conv-name {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
}

.conv-time {
  font-size: 11px;
  color: #64748b;
  flex-shrink: 0;
}

.conv-preview {
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.unread-badge {
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 10px;
  background: #ef4444;
  color: white;
  font-weight: 600;
  min-width: 18px;
  text-align: center;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 18px 24px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.chat-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: white;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
}

.chat-avatar.system {
  background: linear-gradient(135deg, #f59e0b, #ef4444);
  font-size: 22px;
}

.chat-name {
  font-size: 15px;
  font-weight: 600;
  color: #e2e8f0;
}

.chat-status {
  font-size: 12px;
  color: #22c55e;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.msg-bubble {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  max-width: 80%;
  animation: bubbleIn 0.3s ease-out;
}

@keyframes bubbleIn {
  from { opacity: 0; transform: translateY(8px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.msg-bubble.mine {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-bubble.system {
  align-self: center;
  max-width: 90%;
}

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.other-avatar {
  background: linear-gradient(135deg, #64748b, #475569);
}

.mine-avatar {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
}

.bubble-wrap {
  display: flex;
  flex-direction: column;
  max-width: 100%;
}

.bubble-sender-name {
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 3px;
  padding-left: 4px;
}

.msg-bubble.mine .bubble-sender-name {
  display: none;
}

.bubble-text {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.msg-bubble:not(.mine):not(.system) .bubble-text {
  background: rgba(30, 41, 59, 0.8);
  color: #e2e8f0;
  border-bottom-left-radius: 4px;
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.msg-bubble.mine .bubble-text {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
}

.bubble-time {
  font-size: 11px;
  color: #64748b;
  margin-top: 4px;
  padding: 0 4px;
}

.sys-bubble {
  padding: 10px 18px;
  border-radius: 20px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.25);
  color: #fbbf24;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: center;
}

.sys-icon {
  font-size: 16px;
}

.chat-input-area {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
}

.chat-input {
  flex: 1;
  height: 44px;
  padding: 0 16px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.15);
  background: rgba(15, 23, 42, 0.6);
  color: #e2e8f0;
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
}

.chat-input:focus {
  border-color: rgba(96, 165, 250, 0.5);
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.15);
}

.chat-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn {
  height: 44px;
  padding: 0 24px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.45);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-chat, .no-chat-selected, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: #64748b;
  font-size: 14px;
  gap: 10px;
}

.no-chat-selected {
  padding: 40px;
}

.empty-icon {
  font-size: 40px;
  opacity: 0.6;
}

.stagger-children { animation: none; }

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.new-msg-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.25s;
}

.new-msg-btn:hover {
  background: rgba(96, 165, 250, 0.3);
  transform: rotate(-15deg) scale(1.1);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.modal-box {
  width: 440px;
  max-height: 520px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.95), rgba(30, 41, 59, 0.92));
  border: 1px solid rgba(96, 165, 250, 0.25);
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modalPop {
  from { opacity: 0; transform: scale(0.9) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header {
  padding: 18px 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
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

.modal-close:hover { background: rgba(239, 68, 68, 0.2); color: #fca5a5; }

.modal-body {
  padding: 18px 22px 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
}

.user-search-input {
  width: 100%;
  height: 42px;
  padding: 0 16px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(15, 23, 42, 0.7);
  color: #e2e8f0;
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
  box-sizing: border-box;
}

.user-search-input:focus {
  border-color: rgba(96, 165, 250, 0.5);
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.15);
}

.user-search-results {
  max-height: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.search-tip {
  text-align: center;
  color: #64748b;
  font-size: 13px;
  padding: 24px 0;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.25s;
}

.user-item:hover {
  background: rgba(96, 165, 250, 0.1);
  border-color: rgba(96, 165, 250, 0.3);
}

.user-item-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  font-weight: 700;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-item-info {
  flex: 1;
  min-width: 0;
}

.user-item-name {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
}

.real-name {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.user-item-phone {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.start-chat-btn {
  padding: 6px 14px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: white;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  flex-shrink: 0;
}

.start-chat-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

@media (max-width: 768px) {
  .msg-container { flex-direction: column; }
  .sidebar { width: 100%; max-height: 45vh; border-right: none; border-bottom: 1px solid rgba(148, 163, 184, 0.1); }
}
</style>