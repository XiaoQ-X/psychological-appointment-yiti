FROM maven:3.9-eclipse-temurin-17@sha256:1ed5d1f54416b706707b4f3238f63a20bb06aab27c6d240090a2bb9ad895ed45 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy@sha256:475d8e96b4b2bfe08999e5e854755c773af1581acdf959a4545d88f0696a2339
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home app
WORKDIR /app
COPY --from=build /workspace/target/psych-appointment-backend-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
