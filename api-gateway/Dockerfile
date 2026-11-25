# ── Dockerfile – api-gateway ──────────────────────────────────────────
# Le contexte de build est la RACINE du depot (voir docker-compose.yml),
# car chaque service herite du pom parent parcelflow-parent.

# Stage 1 – compilation Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY order-service/pom.xml order-service/
COPY carrier-service/pom.xml carrier-service/
COPY tracking-service/pom.xml tracking-service/
COPY api-gateway/src api-gateway/src
RUN mvn -q -pl api-gateway -am package -DskipTests

# Stage 2 – image d'execution legere (JRE seulement)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S parcelflow && adduser -S parcelflow -G parcelflow
USER parcelflow
COPY --from=builder /build/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
