# ---- build ----------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Dependency layer first so a source-only change doesn't re-download the world.
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- run ------------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Railway injects PORT; application.yml reads it with 8090 as the local default.
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
