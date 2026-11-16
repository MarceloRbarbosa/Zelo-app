# Build stage: compiles the jar with the Maven wrapper. Tests run in CI (they need Docker).
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Run stage: only the JRE and the jar.
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/api-0.0.1-SNAPSHOT.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
# Render tells the port in PORT; locally it stays 8080.
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
