FROM docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre

WORKDIR /app

ENV TZ=Asia/Shanghai

# 安装 Node.js 和 npm（MCP 服务器需要）
RUN apt-get update && apt-get install -y \
    curl \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 预安装 MCP 服务器包（避免运行时下载超时）
# 配置 npm 使用国内镜像源加速下载
RUN npm config set registry https://registry.npmmirror.com \
    && npm install -g @amap/amap-maps-mcp-server

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app/uploads /app/downloads /app/data /app/tmp \
    && chown -R app:app /app

COPY --from=build /workspace/target/*.jar /app/app.jar

USER app

EXPOSE 9000

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
