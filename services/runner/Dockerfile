FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN ./mvnw -B -ntp -DskipTests package

# The runner needs a full JDK at runtime: it compiles learner code (javax.tools)
# and observes the child JVM through JDI (jdk.jdi).
FROM eclipse-temurin:25-jdk
RUN useradd --system --uid 10001 --no-create-home ljbu
USER ljbu
WORKDIR /app
COPY --from=build /workspace/target/ljbu-runner.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
