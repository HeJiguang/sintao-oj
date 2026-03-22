# 指定了基础镜像为openjdk:17.0.2，即使用OpenJDK 17.0.2版本的Java环境作为构建的基础。
FROM openjdk:17.0.2

# 拷贝jar包到容器中
ADD ./oj-gateway.jar ./oj-gateway.jar

# 运行jar包
CMD ["java", "-jar", "oj-gateway.jar"]