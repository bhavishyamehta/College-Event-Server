# ---------- Stage 1: Build ----------
FROM gradle:8.5-jdk21-alpine AS build

WORKDIR /app

COPY . .

RUN gradle shadowJar --no-daemon

# ---------- Stage 2: Run ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S ktorgroup && adduser -S ktoruser -G ktorgroup

# shadowJar output usually ends in -all.jar; adjust if your build produces a different suffix
COPY --from=build /app/build/libs/*-all.jar /app/app.jar

USER ktoruser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]