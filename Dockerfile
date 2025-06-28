# --- Build Stage ---
# Use a Maven image with OpenJDK 21 for building the application.
# `maven:3-openjdk-21` is a commonly available and updated tag on Docker Hub.
FROM maven:3-openjdk-21 AS build

# Set the working directory inside the container for the build stage.
WORKDIR /app

# Copy the parent pom.xml first to leverage Docker's build cache.
# If pom.xml doesn't change, these dependency layers can be reused.
COPY pom.xml .
# Copy individual module pom.xml files.
COPY scms-api/pom.xml scms-api/
COPY scms-core/pom.xml scms-core/
COPY scms-infrastructure/pom.xml scms-infrastructure/

# Download all project dependencies. This helps in caching and speeds up subsequent builds.
# `-B` for batch mode (non-interactive).
RUN mvn dependency:go-offline -B

# Copy all source code into the working directory.
COPY . .

# Build the multi-module application.
# `-DskipTests` is used to skip running tests during the Docker build,
# which can significantly speed up image creation. You might run tests
# in a separate CI/CD pipeline step.
RUN mvn clean install -DskipTests

# --- Runtime Stage ---
# Start a new, smaller image for running the application.
# `openjdk:21-jre-slim-bullseye` provides only the Java Runtime Environment
# based on Debian Bullseye, resulting in a significantly smaller final image.
FROM openjdk:21-jre-slim-bullseye

# Create a dedicated non-root user and group for security best practices.
RUN addgroup --system springuser && adduser --system --ingroup springuser springuser
USER springuser

# Set the working directory inside the container for the runtime.
WORKDIR /app

# Copy the built executable JAR from the 'build' stage into the runtime image.
# Assume 'scms-api' is your main Spring Boot executable module.
# IMPORTANT: Verify 'scms-api-0.0.1-SNAPSHOT.jar' matches the actual JAR name
# found in your 'scms-api/target/' directory after a successful Maven build.
COPY --from=build /app/scms-api/target/scms-api-0001-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot application listens on (default for Spring Boot is 8080).
EXPOSE 8080

# Define the command to run your Spring Boot application when the container starts.
ENTRYPOINT ["java", "-jar", "app.jar"]

# --- Important Considerations for Production / Deployment ---
# 1. JAR Name: Double-check the exact name of your final JAR in `scms-api/target/`
#    after building with Maven and update the `COPY` command if it differs.
#    Common pattern: <artifactId>-<version>.jar (e.g., scms-api-0.0.1-SNAPSHOT.jar).
#    (Corrected to `scms-api-0001-SNAPSHOT.jar` based on an assumption from a previous conversation,
#    please verify this against your actual build output).
# 2. Environment Variables: Avoid hardcoding sensitive data (like database credentials,
#    your encryption key) directly in the Dockerfile or `application.yml` for production.
#    Pass them as environment variables when running the container (e.g., using Docker Compose's `environment` section).
# 3. Database Connection: When using Docker Compose, ensure your Spring Boot
#    application's `spring.datasource.url` points to the *service name* of your
#    database container (e.g., `jdbc:postgresql://db:5432/your_database`).
# 4. Health Checks: For robust deployments, consider adding a `HEALTHCHECK` instruction
#    to verify your application is running and responsive.