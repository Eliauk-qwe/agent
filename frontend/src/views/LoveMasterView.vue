<template>
  <ChatRoom
    app-key="love"
    title="AI 恋爱大师"
    subtitle="专业的恋爱咨询服务"
    assistant-name="恋爱大师"
    chat-id-prefix="love"
    endpoint="/love/chat"
    :include-chat-id="true"
  />
</template>

<script setup>
import ChatRoom from '../components/ChatRoom.vue'
</script>