#!/bin/bash

# 部署脚本
echo "🚀 开始部署..."

# 1. 构建后端 Docker 镜像
echo "📦 构建后端镜像..."
docker build -t ai-agent-backend .

# 2. 部署前端到 Vercel
echo "🌐 部署前端到 Vercel..."
cd frontend
vercel --prod
cd ..

echo "✅ 部署完成！"
echo ""
echo "📝 下一步："
echo "1. 在微信云托管控制台更新环境变量 CORS_ALLOWED_ORIGINS"
echo "2. 在微信云托管控制台重新部署后端"
echo "3. 访问 Vercel 提供的 URL 测试应用"
