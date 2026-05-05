<template>
  <main class="chat-page">
    <!-- 顶部导航栏 -->
    <header class="chat-topbar">
      <RouterLink class="back-link" to="/">
        ← 返回首页
      </RouterLink>
      <div class="chat-title">
        <p class="eyebrow">{{ assistantName }}</p>
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>
      <div class="session-box">
        会话 ID: <strong>{{ chatId }}</strong>
      </div>
    </header>

    <!-- 消息列表 -->
    <section ref="messageListRef" class="message-list" aria-label="聊天记录">
      <article
        v-for="message in messages"
        :key="message.id"
        class="message-row"
        :class="message.role"
      >
        <div class="avatar">{{ message.role === 'user' ? '我' : 'AI' }}</div>
        <div class="message-bubble">
          <div class="message-name">{{ message.role === 'user' ? '我' : assistantName }}</div>
          <details
            v-if="message.role === 'assistant' && message.processSteps?.length"
            class="process-panel"
            open
          >
            <summary>执行过程</summary>
            <ol>
              <li v-for="step in message.processSteps" :key="step.id">
                <div class="process-step-title">{{ step.title }}</div>
                <div class="process-step-content">{{ step.content }}</div>
              </li>
            </ol>
          </details>
          <div
            v-if="message.role === 'assistant'"
            class="message-content markdown-body"
            v-html="renderMarkdown(message.content)"
          ></div>
          <div v-else class="message-content">{{ message.content }}</div>
        </div>
      </article>
    </section>

    <!-- 底部输入区域 -->
    <footer class="composer">
      <button 
        class="toggle-btn" 
        @click="showFileUpload = true"
        title="文件上传"
        aria-label="文件上传"
      >
        📎
      </button>
      <textarea
        ref="inputRef"
        v-model="input"
        rows="1"
        :disabled="isStreaming"
        placeholder="输入消息，Enter 发送，Shift + Enter 换行"
        @keydown.enter.exact.prevent="handleSubmit"
        @input="resizeInput"
      />
      <button 
        v-if="!isStreaming" 
        class="send-button" 
        type="button" 
        :disabled="!input.trim()"
        @click="handleSubmit"
        aria-label="发送消息"
      >
        发送
      </button>
      <button 
        v-else 
        class="stop-button" 
        type="button" 
        @click="stopStream"
        aria-label="停止生成"
      >
        停止
      </button>
    </footer>

    <!-- 文件上传弹窗 -->
    <div v-if="showFileUpload" class="modal-overlay" @click="showFileUpload = false">
      <div class="modal-content file-upload-modal" @click.stop>
        <div class="modal-header">
          <h3>📎 文件上传</h3>
          <button @click="showFileUpload = false" class="btn-close">✕</button>
        </div>
        <div class="modal-body">
          <FileUpload :session-id="chatId" @file-analyzed="handleFileAnalyzed" />
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { createSseUrl } from '../api/http.js'
import { createChatId } from '../utils/session.js'
import FileUpload from './FileUpload.vue'

marked.setOptions({
  breaks: true,
  gfm: true
})

const props = defineProps({
  appKey: {
    type: String,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    required: true
  },
  assistantName: {
    type: String,
    required: true
  },
  chatIdPrefix: {
    type: String,
    required: true
  },
  endpoint: {
    type: String,
    required: true
  },
  includeChatId: {
    type: Boolean,
    default: false
  }
})

const chatId = ref('')
const input = ref('')
const messages = ref([])
const isStreaming = ref(false)
const eventSource = ref(null)
const messageListRef = ref(null)
const inputRef = ref(null)
const showFileUpload = ref(false)

function renderMarkdown(content) {
  return DOMPurify.sanitize(marked.parse(content || ''))
}

function cleanStreamChunk(data) {
  return data.replace(/^Step \d+:\s*/, '')
}

function parseProcessStep(data) {
  const cleanData = cleanStreamChunk(data)
  const [firstLine, ...restLines] = cleanData.split('\n')
  return {
    id: createChatId(`${props.appKey}-process`),
    title: firstLine || '执行步骤',
    content: restLines.join('\n').trim()
  }
}

// 初始化会话
function initSession() {
  chatId.value = createChatId(props.chatIdPrefix)
  messages.value = [
    {
      id: createChatId(`${props.appKey}-welcome`),
      role: 'assistant',
      content: `你好，我是${props.assistantName}！可以开始对话了。\n\n💡 提示：点击左下角的📎按钮可以上传文件，我可以帮你分析文件内容。`
    }
  ]
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 调整输入框高度
function resizeInput() {
  nextTick(() => {
    if (!inputRef.value) return
    inputRef.value.style.height = 'auto'
    inputRef.value.style.height = `${Math.min(inputRef.value.scrollHeight, 200)}px`
  })
}

// 添加消息
function appendMessage(role, content) {
  const message = {
    id: createChatId(`${props.appKey}-${role}`),
    role,
    content,
    processSteps: []
  }
  messages.value.push(message)
  scrollToBottom()
  return message
}

// 构建参数
function buildParams(message) {
  const params = { message }
  if (props.includeChatId) {
    params.chatId = chatId.value
  }
  return params
}

// 提交消息
function handleSubmit() {
  const text = input.value.trim()
  if (!text || isStreaming.value) return

  appendMessage('user', text)
  input.value = ''
  resizeInput()
  startSse(text)
}

// 处理文件分析
function handleFileAnalyzed(fileData) {
  if (fileData.type === 'analyze') {
    showFileUpload.value = false  // 关闭弹窗
    appendMessage('user', fileData.message)
    startSse(fileData.message)
  }
}

// 启动SSE流
function startSse(text) {
  const assistantMessage = appendMessage('assistant', '')
  const url = createSseUrl(props.endpoint, buildParams(text))

  isStreaming.value = true
  let messageContent = ''

  eventSource.value = new EventSource(url)

  eventSource.value.onmessage = (event) => {
    const data = event.data
    const controlText = data.trim()
    
    // 检查是否结束
    if (controlText === '[DONE]' || controlText.includes('执行结束') || controlText.includes('达到最大步骤')) {
      isStreaming.value = false
      // 清理末尾多余的换行
      assistantMessage.content = messageContent.trim()
      return
    }

    if (data.startsWith('[[PROCESS]]')) {
      assistantMessage.processSteps.push(parseProcessStep(data.replace('[[PROCESS]]', '')))
      scrollToBottom()
      return
    }

    const answerData = data.startsWith('[[ANSWER]]') ? data.replace('[[ANSWER]]', '') : data
    messageContent += cleanStreamChunk(answerData)
    assistantMessage.content = messageContent
    scrollToBottom()
  }

  eventSource.value.onerror = () => {
    isStreaming.value = false
    if (!messageContent.trim()) {
      assistantMessage.content = '抱歉，连接中断或服务暂不可用，请稍后重试。'
    } else {
      assistantMessage.content = messageContent.trim()
    }
    stopStream()
  }

  eventSource.value.oncomplete = () => {
    isStreaming.value = false
    assistantMessage.content = messageContent.trim()
    stopStream()
  }
}

// 停止流
function stopStream() {
  if (eventSource.value) {
    eventSource.value.close()
    eventSource.value = null
  }
  isStreaming.value = false
}

// 生命周期
onMounted(() => {
  initSession()
  resizeInput()
})

onBeforeUnmount(() => {
  stopStream()
})
</script>
