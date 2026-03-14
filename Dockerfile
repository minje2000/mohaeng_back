# build stage
FROM gradle:8.7-jdk21 AS builder

WORKDIR /build

COPY . .

RUN gradle clean build -x test

# runtime stage
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
