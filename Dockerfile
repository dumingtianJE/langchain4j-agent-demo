# 多阶段构建 - 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 编译打包(跳过测试)
RUN mvn clean package -DskipTests -B

# 运行阶段 - 使用 Ubuntu 基础镜像（完整 glibc 支持 ONNX Runtime）
FROM eclipse-temurin:21-jre-jammy

# 安装 wget 用于健康检查
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# 创建非 root 用户（Ubuntu 语法）
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=builder /app/target/*.jar app.jar

# 创建数据和日志目录
RUN mkdir -p /app/data /app/logs && chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# JVM 参数优化
ENV JAVA_OPTS="-Xms512m \
               -Xmx1024m \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -Djava.security.egd=file:/dev/./urandom"

# Spring Boot 配置
# 注释掉 production profile，使用默认的 application.yml
# ENV SPRING_PROFILES_ACTIVE=production

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
