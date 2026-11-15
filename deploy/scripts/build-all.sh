#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_DIR"

echo "=============================="
echo "  铁路订票系统 - 一键打包脚本"
echo "=============================="

echo ""
echo "[1/2] 打包后端 JAR..."
# Linux/macOS 使用 mvnw（不是 mvnw.cmd）；prod profile 在运行时由 SPRING_PROFILES_ACTIVE 指定
./mvnw clean package -DskipTests 2>&1 | tail -20

JAR_FILE=$(ls -t target/*.jar 2>/dev/null | grep -v sources | grep -v original | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ 后端打包失败，未找到 JAR 文件"
    exit 1
fi
echo "✅ 后端 JAR: $JAR_FILE"

echo ""
echo "[2/2] 打包前端..."
cd frontend
npm ci
npm run build
cd "$PROJECT_DIR"

if [ ! -d "frontend/dist" ]; then
    echo "❌ 前端打包失败，未找到 dist 目录"
    exit 1
fi
echo "✅ 前端打包完成: frontend/dist"

echo ""
echo "=============================="
echo "  打包全部完成！"
echo "=============================="
echo ""
echo "后端 JAR:  $JAR_FILE"
echo "前端静态:  frontend/dist/"
echo ""
echo "部署到服务器："
echo "  scp $JAR_FILE user@server:/opt/railway/"
echo "  scp -r frontend/dist/* user@server:/var/www/railway-frontend/"
echo "  scp deploy/nginx/railway.conf user@server:/etc/nginx/conf.d/"
echo ""
