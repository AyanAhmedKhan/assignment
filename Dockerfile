FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q verify

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd --system employee && useradd --system --gid employee employee
WORKDIR /app
COPY --from=build --chown=employee:employee /workspace/target/klodspot-assignment-1.1.0.jar app.jar
USER employee
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
