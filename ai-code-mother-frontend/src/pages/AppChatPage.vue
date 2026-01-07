<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import request from '@/request'
import {
  deployApp,
  deleteApp,
  deleteMyApp,
  getAppById,
  getMyAppById,
  listFeaturedAppByPage,
  updateApp,
  updateMyApp,
} from '@/api/appController.ts'
import { useLoginUserStore } from '@/stores/LoginUser.ts'

type ChatMessage = {
  id: string
  role: 'user' | 'assistant'
  content: string
  time: string
}

const escapeHtml = (text: string) =>
  text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

const renderMessageHtml = (msg: ChatMessage) => {
  const content = msg.content || ''
  if (!content) {
    return ''
  }
  if (msg.role === 'assistant') {
    const raw = marked.parse(content, { breaks: true }) as string
    return DOMPurify.sanitize(raw)
  }
  return DOMPurify.sanitize(escapeHtml(content))
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appId = computed(() => String(route.params.id ?? ''))
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const isFeaturedPreview = computed(() => route.query.featured === '1')
const isViewMode = computed(() => route.query.view === '1')
const isPreviewOnly = computed(() => route.query.preview === '1')

const app = ref<API.AppVO>()
const loadingApp = ref(false)

const editVisible = ref(false)
const editSubmitting = ref(false)
const editForm = reactive<{
  appName: string
  cover?: string
  priority?: number
}>({
  appName: '',
  cover: '',
  priority: undefined,
})

const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const streaming = ref(false)
const eventSource = ref<EventSource>()

const previewKey = ref(0)
const deployedUrl = ref('')
const deploying = ref(false)
const detailVisible = ref(false)

const baseStaticOrigin = computed(() => {
  const baseURL = request.defaults.baseURL || ''
  if (!baseURL) {
    return ''
  }
  return baseURL.replace(/\/api\/?$/, '/api/static')
})

const previewUrl = computed(() => {
  if (!app.value || !appId.value || !baseStaticOrigin.value) {
    return ''
  }
  const type = app.value.codeGenType || 'html'
  return `${baseStaticOrigin.value}/${type}_${appId.value}/`
})

const pageTitle = computed(() => app.value?.appName || '未命名应用')

const isOwner = computed(() => {
  if (!app.value?.userId || !loginUserStore.loginUser.id) {
    return false
  }
  return app.value.userId === loginUserStore.loginUser.id
})

const canChat = computed(() => isOwner.value)
const canManage = computed(() => isOwner.value || isAdmin.value)

const nowText = () => {
  const d = new Date()
  const pad = (n: number) => (n < 10 ? `0${n}` : String(n))
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const appendMessage = (msg: ChatMessage) => {
  messages.value.push(msg)
}

const updateAssistantContent = (id: string, chunk: string) => {
  const target = messages.value.find((m) => m.id === id)
  if (target) {
    target.content += chunk
  }
}

const closeEventSource = () => {
  if (eventSource.value) {
    eventSource.value.close()
    eventSource.value = undefined
  }
  streaming.value = false
}

const refreshPreview = () => {
  if (previewUrl.value) {
    previewKey.value += 1
  }
}

const startChat = (rawMessage?: string) => {
  const content = (rawMessage ?? inputMessage.value).trim()
  if (!content) {
    message.warning('请输入需求描述')
    return
  }
  if (!appId.value) {
    message.error('应用信息缺失')
    return
  }
  if (streaming.value) {
    message.warning('正在生成中，请稍候')
    return
  }

  const userMsg: ChatMessage = {
    id: `user_${Date.now()}`,
    role: 'user',
    content,
    time: nowText(),
  }
  appendMessage(userMsg)

  const assistantId = `assistant_${Date.now()}`
  const assistantMsg: ChatMessage = {
    id: assistantId,
    role: 'assistant',
    content: '',
    time: nowText(),
  }
  appendMessage(assistantMsg)

  inputMessage.value = ''
  streaming.value = true

  const baseURL = request.defaults.baseURL || ''
  const url = `${baseURL}/app/chat/gen/code?appId=${encodeURIComponent(
    appId.value,
  )}&message=${encodeURIComponent(content)}`

  const es = new EventSource(url, { withCredentials: true } as EventSourceInit)
  eventSource.value = es

  es.onmessage = (event) => {
    const rawData = event.data || ''
    const trimmed = rawData.trim()
    if (trimmed === '[DONE]' || trimmed === '__DONE__') {
      closeEventSource()
      refreshPreview()
      return
    }
    let contentChunk = rawData
    try {
      const parsed = JSON.parse(rawData)
      if (parsed && typeof parsed === 'object' && 'd' in parsed) {
        contentChunk = String((parsed as { d?: unknown }).d ?? '')
      }
    } catch {
      contentChunk = rawData
    }
    if (contentChunk) {
      updateAssistantContent(assistantId, contentChunk)
    }
  }

  es.onerror = () => {
    closeEventSource()
    refreshPreview()
  }
}

const handleSend = () => {
  startChat()
}

const handleDeploy = async () => {
  if (!appId.value) {
    message.error('应用信息缺失')
    return
  }
  if (deploying.value) {
    return
  }
  deploying.value = true
  try {
    const res = await deployApp({
      appId: appId.value as unknown as number,
    })
    if (res.data.code === 0 && res.data.data) {
      deployedUrl.value = res.data.data
      message.success('部署成功')
    } else {
      message.error('部署失败' + res.data.message)
    }
  } finally {
    deploying.value = false
  }
}

const initEditFormFromApp = () => {
  if (!app.value) {
    return
  }
  editForm.appName = app.value.appName || ''
  editForm.cover = app.value.cover || ''
  editForm.priority = app.value.priority ?? undefined
}

const openEdit = async () => {
  if (!appId.value) {
    message.error('应用信息缺失')
    return
  }
  if (!app.value) {
    await fetchApp()
  }
  if (!app.value) {
    message.error('应用信息缺失')
    return
  }
  initEditFormFromApp()
  editVisible.value = true
}

const openDetail = () => {
  detailVisible.value = true
}

const handleEditOk = async () => {
  if (!appId.value) {
    message.error('应用信息缺失')
    return
  }
  if (!editForm.appName.trim()) {
    message.warning('请输入应用名称')
    return
  }
  editSubmitting.value = true
  try {
    if (isAdmin.value) {
      const res = await updateApp({
        id: appId.value as unknown as number,
        appName: editForm.appName,
        cover: editForm.cover,
        priority: editForm.priority,
      })
      if (res.data.code === 0) {
        message.success('保存成功')
        if (app.value) {
          app.value.appName = editForm.appName
          app.value.cover = editForm.cover
          app.value.priority = editForm.priority
        }
        editVisible.value = false
      } else {
        message.error('保存失败' + res.data.message)
      }
    } else {
      const res = await updateMyApp({
        id: appId.value as unknown as number,
        appName: editForm.appName,
        cover: editForm.cover,
      })
      if (res.data.code === 0) {
        message.success('保存成功')
        if (app.value) {
          app.value.appName = editForm.appName
          app.value.cover = editForm.cover
        }
        editVisible.value = false
      } else {
        message.error('保存失败' + res.data.message)
      }
    }
  } finally {
    editSubmitting.value = false
  }
}

const handleEditCancel = () => {
  editVisible.value = false
}

const handleDetailEdit = () => {
  detailVisible.value = false
  openEdit()
}

const handleDetailDelete = async () => {
  if (!appId.value) {
    message.error('应用信息缺失')
    return
  }
  try {
    if (isAdmin.value) {
      const res = await deleteApp({
        id: appId.value as unknown as number,
      })
      if (res.data.code === 0) {
        message.success('删除成功')
        detailVisible.value = false
        router.push('/')
      } else {
        message.error('删除失败' + res.data.message)
      }
    } else {
      const res = await deleteMyApp({
        id: appId.value as unknown as number,
      })
      if (res.data.code === 0) {
        message.success('删除成功')
        detailVisible.value = false
        router.push('/')
      } else {
        message.error('删除失败' + res.data.message)
      }
    }
  } catch {
    message.error('删除失败')
  }
}

const fetchApp = async () => {
  if (!appId.value) {
    message.error('应用信息缺失')
    return
  }
  loadingApp.value = true
  try {
    if (isFeaturedPreview.value) {
      const res = await listFeaturedAppByPage({
        pageNum: 1,
        pageSize: 1,
        id: appId.value as unknown as number,
      })
      if (res.data.code === 0 && res.data.data && res.data.data.records?.length) {
        app.value = res.data.data.records[0]
      } else {
        message.error('获取应用失败' + res.data.message)
      }
    } else if (isAdmin.value) {
      const res = await getAppById({ id: appId.value as unknown as number })
      if (res.data.code === 0 && res.data.data) {
        app.value = res.data.data
      } else {
        message.error('获取应用失败' + res.data.message)
      }
    } else {
      const res = await getMyAppById({ id: appId.value as unknown as number })
      if (res.data.code === 0 && res.data.data) {
        app.value = res.data.data
      } else {
        message.error('获取应用失败' + res.data.message)
      }
    }
  } finally {
    loadingApp.value = false
  }
}

const autoStartIfNeeded = () => {
  if (isViewMode.value || isPreviewOnly.value) {
    return
  }
  const autoStart = route.query.autoStart === '1'
  if (!autoStart || !app.value?.initPrompt) {
    return
  }
  startChat(app.value.initPrompt)
}

onMounted(async () => {
  await fetchApp()
  autoStartIfNeeded()
})

onBeforeUnmount(() => {
  closeEventSource()
})
</script>

<template>
  <div class="app-chat-page">
    <a-spin :spinning="loadingApp">
      <div class="app-chat-header">
        <div class="app-chat-title">
          <div class="app-name">{{ pageTitle }}</div>
          <div class="app-meta">
            <span v-if="app?.codeGenType">模式：{{ app.codeGenType }}</span>
            <span v-if="app?.createTime">创建于 {{ app.createTime }}</span>
          </div>
        </div>
        <div v-if="!isFeaturedPreview" class="app-chat-actions">
          <a-space>
            <a-button @click="openDetail">应用详情</a-button>
            <a-button type="primary" :loading="deploying" @click="handleDeploy">
              部署应用
            </a-button>
            <a-button v-if="deployedUrl" type="link" :href="deployedUrl" target="_blank">
              打开已部署站点
            </a-button>
          </a-space>
        </div>
      </div>

      <div v-if="!isPreviewOnly" class="app-chat-body">
        <div class="chat-pane">
          <div class="chat-messages">
            <template v-if="messages.length">
              <div
                v-for="item in messages"
                :key="item.id"
                class="chat-message"
                :class="item.role === 'user' ? 'chat-message-user' : 'chat-message-assistant'"
              >
                <div class="chat-message-meta">
                  <span class="chat-message-role">
                    {{ item.role === 'user' ? '我' : 'AI' }}
                  </span>
                  <span class="chat-message-time">{{ item.time }}</span>
                </div>
                <div class="chat-message-content" v-html="renderMessageHtml(item)"></div>
              </div>
            </template>
            <a-empty v-else description="还没有对话，试着在下方输入你的需求吧" />
          </div>
          <div class="chat-input">
            <a-tooltip :title="!canChat ? '无法在别人的作品下对话哦~' : ''">
              <a-textarea
                v-model:value="inputMessage"
                class="chat-input-area"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                :disabled="streaming || !canChat"
                placeholder="描述你的需求，比如：帮我生成一个个人博客网站……"
              />
            </a-tooltip>
            <div class="chat-input-footer">
              <div class="chat-input-tip">回车发送，正在生成时暂不支持发送新的消息</div>
              <a-button
                type="primary"
                :loading="streaming"
                :disabled="!canChat"
                @click="handleSend"
              >
                发送
              </a-button>
            </div>
          </div>
        </div>

        <div class="preview-pane">
          <div class="preview-header">生成后的网站展示</div>
          <div class="preview-body">
            <iframe
              v-if="previewUrl"
              :key="previewKey"
              :src="previewUrl"
              class="preview-iframe"
              frameborder="0"
            />
            <a-empty v-else description="暂无可预览的页面" />
          </div>
        </div>
      </div>

      <div v-else class="featured-preview-body">
        <div class="featured-preview-card">
          <iframe
            v-if="previewUrl"
            :key="previewKey"
            :src="previewUrl"
            class="featured-preview-iframe"
            frameborder="0"
          />
          <a-empty v-else description="暂无可预览的页面" />
        </div>
      </div>

      <a-modal
        v-model:open="editVisible"
        title="编辑应用"
        :confirm-loading="editSubmitting"
        @ok="handleEditOk"
        @cancel="handleEditCancel"
      >
        <a-form :model="editForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
          <a-form-item label="应用名称">
            <a-input v-model:value="editForm.appName" placeholder="请输入应用名称" />
          </a-form-item>
          <a-form-item label="封面地址">
            <div style="display: flex; align-items: center; gap: 16px">
              <a-input v-model:value="editForm.cover" placeholder="请输入封面图片地址" />
              <a-image
                v-if="editForm.cover"
                :src="editForm.cover"
                style="width: 36px; height: 36px; object-fit: cover"
              />
            </div>
          </a-form-item>
          <a-form-item v-if="isAdmin" label="优先级">
            <a-input-number
              v-model:value="editForm.priority"
              :min="0"
              :max="999"
              style="width: 160px"
              placeholder="数值越大优先级越高"
            />
          </a-form-item>
        </a-form>
      </a-modal>
      <a-modal v-model:open="detailVisible" title="应用详情" :footer="null">
        <div class="app-detail-content">
          <div class="app-detail-row">
            <span class="app-detail-label">创建者：</span>
            <div class="app-detail-user" v-if="app?.userVO">
              <a-avatar :src="app.userVO.userAvatar">
                {{ app.userVO.userName?.[0] || app.userVO.userAccount?.[0] || '用' }}
              </a-avatar>
              <span class="app-detail-user-name">
                {{ app.userVO.userName || app.userVO.userAccount || '未知用户' }}
              </span>
            </div>
            <span v-else>-</span>
          </div>
          <div class="app-detail-row">
            <span class="app-detail-label">创建时间：</span>
            <span>{{ app?.createTime || '-' }}</span>
          </div>
          <div v-if="canManage" class="app-detail-actions">
            <a-space>
              <a-button type="primary" @click="handleDetailEdit">修改</a-button>
              <a-popconfirm
                title="确定删除该应用吗？"
                ok-text="删除"
                cancel-text="取消"
                @confirm="handleDetailDelete"
              >
                <a-button danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </div>
        </div>
      </a-modal>
    </a-spin>
  </div>
</template>

<style scoped>
.app-chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
}

.app-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.app-chat-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.app-name {
  font-size: 20px;
  font-weight: 600;
}

.app-meta {
  font-size: 12px;
  color: #999999;
  display: flex;
  gap: 12px;
}

.app-detail-content {
  padding-top: 8px;
}

.app-detail-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.app-detail-label {
  width: 80px;
  color: #666666;
}

.app-detail-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-detail-user-name {
  font-weight: 500;
}

.app-detail-actions {
  margin-top: 16px;
}

.app-chat-body {
  display: grid;
  grid-template-columns: minmax(0, 2.2fr) minmax(0, 3fr);
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.chat-pane {
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.chat-messages {
  height: 320px;
  overflow-y: auto;
  padding-right: 8px;
}

.chat-message {
  margin-bottom: 8px;
  max-width: 100%;
  display: flex;
  flex-direction: column;
}

.chat-message-user {
  align-items: flex-end;
}

.chat-message-assistant {
  align-items: flex-start;
}

.chat-message-user .chat-message-meta {
  justify-content: flex-end;
}

.chat-message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
}

.chat-message-role {
  font-weight: 500;
}

.chat-message-time {
  color: #999999;
}

.chat-message-content {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.4;
  word-break: break-word;
  white-space: pre-wrap;
}

.chat-message-user .chat-message-content {
  background: #1677ff;
  color: #ffffff;
  margin-left: auto;
}

.chat-message-assistant .chat-message-content {
  background: #f5f5f5;
  color: #333333;
  margin-right: auto;
}

.chat-input {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
  margin-top: 8px;
}

.chat-input-area {
  margin-bottom: 8px;
}

.chat-input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-input-tip {
  font-size: 12px;
  color: #999999;
}

.preview-pane {
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.preview-header {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
}

.preview-body {
  flex: 1;
  min-height: 0;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 12px;
  background: #f5f5f5;
}

.featured-preview-body {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 0;
}

.featured-preview-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  max-width: 960px;
  width: 100%;
}

.featured-preview-iframe {
  width: 100%;
  height: calc(100vh - 160px);
  border: none;
  border-radius: 12px;
  background: #f5f5f5;
}

.featured-preview-cover {
  position: relative;
  padding-top: 56%;
  background: #f5f5f5;
  border-radius: 12px;
  overflow: hidden;
}

.featured-preview-cover img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.featured-preview-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999999;
  font-size: 14px;
}

@media (max-width: 992px) {
  .app-chat-body {
    grid-template-columns: minmax(0, 1fr);
  }

  .preview-pane {
    min-height: 300px;
  }
}
</style>
