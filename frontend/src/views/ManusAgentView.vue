<template>
  <ChatRoom
    app-key="manus"
    title="AI 超级智能体"
    subtitle="多功能智能助手"
    assistant-name="超级智能体"
    chat-id-prefix="manus"
    endpoint="/manus/chat"
    :include-chat-id="true"
  />
</template>

<script setup>
import ChatRoom from '../components/ChatRoom.vue'
</script>