# ── Dockerfile – API Gateway ──────────────────────────────────
# Multi-stage build : réduit la taille de l'image finale
# Stage 1 : compilation Maven
# Stage 2 : image JRE minimale pour l'exécution

# Stage 1 – Builder
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copier d'abord les POM pour profiter du cache Docker des dépendances
COPY pom.xml .
# Note: le parent POM doit être installé – géré par docker-compose build context

COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2 – Runner (JRE léger)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Utilisateur non-root pour la sécurité
RUN addgroup -S parcelflow && adduser -S parcelflow -G parcelflow
USER parcelflow

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
