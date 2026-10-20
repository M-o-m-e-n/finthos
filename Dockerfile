# syntax=docker/dockerfile:1

# ---- build: compile and package the Spring Boot jar --------------------------------------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Dependencies first, so they stay cached until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
# Tests need Docker (Testcontainers), so they run outside the image: ./mvnw clean verify
RUN ./mvnw -q -B package -DskipTests

# ---- runtime: just the JRE and the jar ------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 finthos
COPY --from=build /workspace/target/finthos-*.jar app.jar
USER finthos
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
