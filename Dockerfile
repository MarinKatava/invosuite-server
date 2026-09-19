# --- Build stage -------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies separately from source so `docker build` doesn't
# re-download the world every time only the source changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# --- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S invoisuite && adduser -S invoisuite -G invoisuite
COPY --from=build /build/target/invoisuite-server-*.jar app.jar
RUN chown invoisuite:invoisuite app.jar
USER invoisuite

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
