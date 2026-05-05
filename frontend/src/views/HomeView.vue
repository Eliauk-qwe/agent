<template>
  <main class="home-page">
    <section class="home-shell" aria-label="应用启动台">
      <div class="home-hero">
        <div class="brand-mark">A+</div>
        <p class="eyebrow">AI Agent Plus</p>
        <h1>今天想让谁帮你？</h1>
        <p class="hero-copy">选择一个智能体开始独立会话。</p>
      </div>

      <section class="app-grid" aria-label="应用列表">
        <RouterLink class="app-card love-card" to="/love-master">
          <span class="app-topline">
            <span class="app-icon">恋</span>
            <span class="app-status">情感对话</span>
          </span>
          <span class="app-name">AI 恋爱大师</span>
          <span class="app-desc">关系梳理、沟通措辞、聊天记录分析</span>
          <span class="app-footer">
            <span>进入会话</span>
            <span class="app-arrow">→</span>
          </span>
        </RouterLink>

        <RouterLink class="app-card agent-card" to="/manus-agent">
          <span class="app-topline">
            <span class="app-icon">智</span>
            <span class="app-status">工具执行</span>
          </span>
          <span class="app-name">AI 超级智能体</span>
          <span class="app-desc">文件分析、网页抓取、PDF 生成</span>
          <span class="app-footer">
            <span>进入会话</span>
            <span class="app-arrow">→</span>
          </span>
        </RouterLink>
      </section>
    </section>
  </main>
</template>

<script>
export default {
  name: 'HomeView'
}
</script>
