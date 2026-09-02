# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY pom.xml ./

RUN --mount=type=cache,target=/root/.m2/repository \
    bash -c 'for attempt in 1 2 3 4 5; do \
      mvn -B -DskipTests dependency:go-offline && exit 0; \
      echo "Maven dependency:go-offline attempt ${attempt} failed, retrying in 30s..."; \
      sleep 30; \
    done; exit 1'

COPY src ./src

RUN --mount=type=cache,target=/root/.m2/repository \
    bash -c 'for attempt in 1 2 3 4 5; do \
      mvn -B -DskipTests package && exit 0; \
      echo "Maven package attempt ${attempt} failed, retrying in 30s..."; \
      sleep 30; \
    done; exit 1'

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring

COPY --from=build /app/target/mediagenda-bot-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
