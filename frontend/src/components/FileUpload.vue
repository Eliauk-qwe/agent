<template>
  <div class="file-upload-container">
    <!-- 上传区域 -->
    <div 
      class="upload-area" 
      :class="{ 'drag-over': isDragOver }"
      @drop="handleDrop"
      @dragover.prevent="isDragOver = true"
      @dragleave="isDragOver = false"
      @click="triggerFileInput"
    >
      <input 
        ref="fileInput"
        type="file" 
        multiple 
        @change="handleFileSelect"
        style="display: none;"
      >
      
      <div class="upload-icon">📁</div>
      <div class="upload-text">
        <p><strong>点击上传文件</strong> 或拖拽文件到此处</p>
        <p class="upload-hint">支持任意文本文件，以及图片、PDF、Word 等格式 • 单个文件最大10MB</p>
      </div>
    </div>

    <!-- 上传进度 -->
    <div v-if="uploading" class="upload-progress">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
      </div>
      <p>上传中... {{ uploadProgress }}%</p>
    </div>

    <!-- 已上传文件列表 -->
    <div v-if="uploadedFiles.length > 0" class="uploaded-files">
      <h3>已上传文件 ({{ uploadedFiles.length }})</h3>
      <div class="file-list">
        <div v-for="file in uploadedFiles" :key="file.fileId" class="file-item">
          <div class="file-info">
            <div class="file-icon">{{ getFileIcon(file.fileType) }}</div>
            <div class="file-details">
              <div class="file-name">{{ file.originalName }}</div>
              <div class="file-meta">
                {{ formatFileSize(file.fileSize) }} • {{ formatDate(file.uploadTime) }}
                <span v-if="file.hasText" class="text-available">📄 可分析</span>
              </div>
            </div>
          </div>
          <div class="file-actions">
            <button @click="viewFile(file)" class="btn-view" title="查看">👁️</button>
            <button @click="analyzeFile(file)" class="btn-analyze" title="AI分析">🤖</button>
            <button @click="deleteFile(file)" class="btn-delete" title="删除">🗑️</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 文件预览模态框 -->
    <div v-if="previewFile" class="modal-overlay" @click="closePreview">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ previewFile.originalName }}</h3>
          <button @click="closePreview" class="btn-close">✕</button>
        </div>
        <div class="modal-body">
          <!-- 图片预览 -->
          <img 
            v-if="previewFile.fileType === 'image'" 
            :src="getFileUrl(previewFile.fileId)" 
            :alt="previewFile.originalName"
            class="preview-image"
          >
          
          <!-- 文本内容预览 -->
          <div v-else-if="previewFile.hasText && previewFile.extractedText" class="preview-text">
            <pre>{{ previewFile.extractedText }}</pre>
          </div>
          
          <!-- 其他文件类型 -->
          <div v-else class="preview-placeholder">
            <p>无法预览此文件类型</p>
            <a :href="getFileUrl(previewFile.fileId)" target="_blank" class="btn-download">
              下载文件
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http.js'

const props = defineProps({
  sessionId: {
    type: String,
    default: 'default'
  }
})

const emit = defineEmits(['file-analyzed'])

const fileInput = ref(null)
const isDragOver = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadedFiles = ref([])
const previewFile = ref(null)

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value.click()
}

// 处理文件选择
const handleFileSelect = (event) => {
  const files = Array.from(event.target.files)
  uploadFiles(files)
}

// 处理拖拽上传
const handleDrop = (event) => {
  event.preventDefault()
  isDragOver.value = false
  const files = Array.from(event.dataTransfer.files)
  uploadFiles(files)
}

// 上传文件
const uploadFiles = async (files) => {
  if (files.length === 0) return

  uploading.value = true
  uploadProgress.value = 0

  try {
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      const formData = new FormData()
      formData.append('file', file)
      formData.append('sessionId', props.sessionId)

      const response = await http.post('/files/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          uploadProgress.value = Math.round(((i + progress / 100) / files.length) * 100)
        }
      })

      if (response.data.success) {
        console.log('文件上传成功:', response.data.data)
      } else {
        throw new Error(response.data.message)
      }
    }

    await loadUploadedFiles()
    fileInput.value.value = ''
    
  } catch (error) {
    console.error('文件上传失败:', error)
    alert('文件上传失败: ' + (error.response?.data?.message || error.message))
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

// 加载已上传文件列表
const loadUploadedFiles = async () => {
  try {
    const response = await http.get(`/files/session/${props.sessionId}`)
    if (response.data.success) {
      uploadedFiles.value = response.data.data
    }
  } catch (error) {
    console.error('加载文件列表失败:', error)
  }
}

// 查看文件
const viewFile = async (file) => {
  try {
    const response = await http.get(`/files/${file.fileId}/info`)
    if (response.data.success) {
      previewFile.value = response.data.data
    }
  } catch (error) {
    console.error('获取文件信息失败:', error)
    previewFile.value = file
  }
}

// 让AI分析文件
const analyzeFile = (file) => {
  const message = `我上传了一个文件，请帮我分析它的内容。

文件信息：
- 文件名：${file.originalName}
- 文件ID：${file.fileId}
- 会话ID：${props.sessionId}

请使用 analyzeFile 工具读取文件ID为 ${file.fileId} 的文件内容，然后分析并总结文件的主要内容。`

  emit('file-analyzed', {
    type: 'analyze',
    fileId: file.fileId,
    fileName: file.originalName,
    sessionId: props.sessionId,
    message: message
  })
}

// 删除文件
const deleteFile = async (file) => {
  if (!confirm(`确定要删除文件"${file.originalName}"吗？`)) return

  try {
    const response = await http.delete(`/files/${file.fileId}`)
    if (response.data.success) {
      await loadUploadedFiles()
    } else {
      throw new Error(response.data.message)
    }
  } catch (error) {
    console.error('删除文件失败:', error)
    alert('删除文件失败: ' + (error.response?.data?.message || error.message))
  }
}

// 关闭预览
const closePreview = () => {
  previewFile.value = null
}

// 获取文件URL
const getFileUrl = (fileId) => {
  return `/api/files/${fileId}/download`
}

// 获取文件图标
const getFileIcon = (fileType) => {
  const icons = {
    image: '🖼️',
    document: '📄',
    text: '📝'
  }
  return icons[fileType] || '📁'
}

// 格式化文件大小
const formatFileSize = (size) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

// 格式化日期
const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

// 组件挂载时加载文件列表
onMounted(() => {
  loadUploadedFiles()
})
</script>
