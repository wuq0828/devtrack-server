# 多阶段构建:Maven 编译 -> Corretto 17 运行(对齐团队 amazoncorretto 基础镜像)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY devtrack-common/pom.xml devtrack-common/
COPY devtrack-app/pom.xml devtrack-app/
RUN mvn -B -q dependency:go-offline || true
COPY . .
RUN mvn -B -DskipTests package

FROM amazoncorretto:17
WORKDIR /home/work/server
COPY --from=build /app/devtrack-app/target/devtrack-app.jar ./app.jar
EXPOSE 8080
# JDK 22 才需 bytebuddy.experimental;基础镜像是 17,这里保留无害
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=70", \
            "-jar", "app.jar", "--spring.profiles.active=prod"]
