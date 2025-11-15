# Cafeteria Service Dockerfile
# 多阶段构建：减小镜像体积，提高安全性

# ==================== 构建阶段 ====================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 和父项目依赖（利用 Docker 缓存）
COPY pom.xml .

# 下载依赖（仅当 pom.xml 变化时重新下载）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用（跳过测试）
RUN mvn clean package -DskipTests -B

# ==================== 运行阶段 ====================
FROM eclipse-temurin:17-jre-alpine

# 安装必要的工具
RUN apk add --no-cache curl tzdata

# 设置时区为 Asia/Singapore
ENV TZ=Asia/Singapore
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建应用用户（非root用户，提高安全性）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 设置工作目录
WORKDIR /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 创建日志目录 (与 logback-spring.xml 中的路径一致)
RUN mkdir -p /app/logs /var/log/cafeteria-service && \
    chown -R appuser:appgroup /app/logs /var/log/cafeteria-service

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8083 9083

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

# 启动应用
# 使用环境变量配置 JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
