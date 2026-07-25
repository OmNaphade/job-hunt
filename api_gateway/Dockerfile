FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -f api_gateway/pom.xml -DskipTests package org.springframework.boot:spring-boot-maven-plugin:repackage
RUN JAR_PATH=$(find api_gateway/target -maxdepth 1 -name "*.jar" ! -name "*.jar.original" | head -n 1) && cp "$JAR_PATH" /workspace/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
