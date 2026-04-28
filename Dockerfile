# ============================================================
# OpenRoof — Dockerfile (multi-stage, Java 25)
# ============================================================

# ─────────────────────────────────────────────────
# Stage 1: Build
# ─────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copiar wrapper y pom primero → mejor caché de dependencias
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copiar fuentes y compilar
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# ─────────────────────────────────────────────────
# Stage 2: Runtime (imagen mínima)
# ─────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS runtime

WORKDIR /app

# Usuario sin privilegios root
RUN addgroup -S openroof && adduser -S openroof -G openroof

# Copiar JAR del stage anterior
COPY --from=builder /app/target/openroof-0.0.1-SNAPSHOT.jar app.jar

USER openroof

# Render inyecta la variable PORT; Spring la lee como SERVER_PORT en application-prod.yml
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java -Xmx256m -Xms128m -Xss512k -XX:MaxMetaspaceSize=128m -XX:ActiveProcessorCount=1 -XX:+ExitOnOutOfMemoryError -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -jar app.jar"]
