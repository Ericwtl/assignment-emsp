FROM openjdk:17
#Important solve the different timezone problem.
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# 设置工作目录
WORKDIR /app
# 复制编译后的 JAR 文件
COPY target/assignment-0.0.1-SNAPSHOT.jar app.jar
# 暴露端口
EXPOSE 8080
# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
