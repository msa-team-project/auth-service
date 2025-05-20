# 빌드 스테이지: Gradle + JDK 21 (Alpine)
FROM gradle:8.7.0-jdk21-alpine AS builder

WORKDIR /workspace

COPY build.gradle .
COPY settings.gradle .

RUN gradle wrapper

RUN ./gradlew dependencies

COPY src src

RUN ./gradlew build -x test

# 런타임 스테이지: OpenJDK 21 (Alpine)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /workspace

COPY --from=builder /workspace/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
