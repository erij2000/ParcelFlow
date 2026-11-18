# ── Dockerfile – Tracking Service ─────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S parcelflow && adduser -S parcelflow -G parcelflow
USER parcelflow
COPY --from=builder /build/target/*.jar app.jar
# Port HTTP et port gRPC
EXPOSE 8083 9090
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
