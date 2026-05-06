# 部署配置记录

## 后端信息

### 微信云托管
- **后端 URL**: `https://________________.weixincloud.com`
- **部署时间**: _______________
- **状态**: [ ] 部署中 [ ] 已部署 [ ] 运行中

### 环境变量（微信云托管控制台配置）
```bash
# 第一次部署（临时配置）
CORS_ALLOWED_ORIGINS=https://*.vercel.app,http://localhost:*

# 前端部署后更新为（精确配置）
CORS_ALLOWED_ORIGINS=https://你的前端URL,https://你的前端URL-*.vercel.app,http://localhost:*

# API Keys
DASHSCOPE_API_KEY=sk-b038c963102742cfa5934a91d8351045
SEARCH_API_KEY=up1VWzxSHxEQFgemPN5sRsi4

# 数据库配置
MYSQL_URL=jdbc:mysql://________________:3306/ai_agent
MYSQL_USERNAME=________________
MYSQL_PASSWORD=________________

PGVECTOR_URL=jdbc:postgresql://________________:5432/agent
PGVECTOR_USERNAME=________________
PGVECTOR_PASSWORD=________________
```

---

## 前端信息

### Vercel
- **前端 URL**: `https://________________.vercel.app`
- **部署时间**: _______________
- **状态**: [ ] 部署中 [ ] 已部署 [ ] 运行中

### 环境变量（Vercel 控制台配置）
```bash
VITE_API_BASE_URL=https://你的后端URL/api
```

---

## 部署检查清单

### 后端
- [ ] Docker 镜像构建成功
- [ ] 推送到微信云托管成功
- [ ] 环境变量配置完成
- [ ] 服务启动成功
- [ ] 健康检查接口可访问：`curl https://你的后端URL/api/health`

### 前端
- [ ] `.env.production` 配置正确
- [ ] `vercel.json` 配置正确
- [ ] 部署到 Vercel 成功
- [ ] 页面可以访问
- [ ] API 调用正常（无 CORS 错误）

### 功能测试
- [ ] 聊天功能正常
- [ ] 文件上传功能正常
- [ ] MCP 工具调用正常
- [ ] RAG 检索功能正常

---

## 故障排查

### 问题 1：CORS 错误
**检查**：
1. 后端环境变量 `CORS_ALLOWED_ORIGINS` 是否包含前端 URL
2. 前端 URL 是否正确（包括 https://）
3. 后端是否重新部署

### 问题 2：API 404
**检查**：
1. 前端 `.env.production` 中的 URL 是否正确
2. URL 是否包含 `/api` 后缀
3. 后端服务是否正常运行

### 问题 3：连接超时
**检查**：
1. 后端服务是否启动
2. 网络是否正常
3. 防火墙设置

---

## 更新记录

| 日期 | 操作 | 备注 |
|------|------|------|
|      | 初次部署 |      |
|      | 更新 CORS |      |
|      |      |      |
