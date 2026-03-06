# ── Stage 1: Build ───────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia frontend para os recursos estáticos do Spring Boot
COPY frontend/ src/main/resources/static/

# Baixa dependências antes de copiar o código (cache de camadas)
COPY backend/pom.xml pom.xml
RUN mvn dependency:go-offline -q

# Compila
COPY backend/src src/
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/frota-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
