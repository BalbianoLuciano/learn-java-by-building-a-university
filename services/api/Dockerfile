# Build context: the repository root. The api packages the JSON Schemas of packages/contracts
# and serves the content of content/.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /repo/services/api
COPY packages/contracts/*.schema.json /repo/packages/contracts/
COPY services/api/mvnw services/api/pom.xml ./
COPY services/api/.mvn .mvn
COPY services/api/src/main src/main
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --no-create-home ljbu
USER ljbu
WORKDIR /app
COPY --from=build /repo/services/api/target/ljbu-api.jar app.jar
COPY content content
ENV CONTENT_DIR=/app/content
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
