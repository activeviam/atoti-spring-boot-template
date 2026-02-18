# ── Base image: JDK 21 on Ubuntu Jammy (linux/amd64 compatible with minikube) ──
#openjdk:21-jdk-slim
FROM eclipse-temurin:21-jre-jammy

# ── Working directory inside the container ────────────────────────────────────
WORKDIR /app

# ── Copy the executable JAR from your build output ────────────────────────────
COPY target/atoti-spring-boot-template.jar atoti-app.jar
COPY src/main/resources/data/ data

# ── Create directories for mounted volume and logs ────────────────────────────
RUN mkdir -p /app/logs /app/data

# ── Volume mount point — local disk will be mounted here at runtime ───────────
VOLUME /app/data

# ── Expose Atoti default port ─────────────────────────────────────────────────
EXPOSE 9090

# ── ATOTI_LICENSE is passed in at runtime via -e or docker-compose ─────────────
# Declared here so it is visible as a documented contract of this image.
ENV ATOTI_LICENSE=""

# ── Start the application ─────────────────────────────────────────────────────
ENTRYPOINT ["java", \
  "-Xms1g", \
  "-Xmx1g", \
  "-XX:+UseG1GC", \
  "-jar", "atoti-app.jar", "--spring.profiles.active=local", "--CSV_DATA_BASE_DIR=/app/data"]