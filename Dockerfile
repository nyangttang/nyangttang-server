FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/
# bootJar compiles the application without needing a running database at build time.
# Run the tests separately before deployment.
RUN chmod +x gradlew && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app
# A clean bootJar build produces one executable jar; the host build/ is not copied.
COPY --from=build --chown=app:app /workspace/build/libs/*.jar /app/app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0 -Duser.timezone=UTC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
