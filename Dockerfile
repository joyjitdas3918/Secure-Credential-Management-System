# --- Build Stage ---
FROM maven:3.9.6-openjdk-21 AS build
WORKDIR /app

# Copy the parent pom.xml and download dependencies
# This leverages Docker cache for dependencies
COPY pom.xml .
COPY scms-api/pom.xml scms-api/
COPY scms-core/pom.xml scms-core/
COPY scms-infrastructure/pom.xml scms-infrastructure/

# Use the "go-offline" goal to download all dependencies
# This is crucial for caching and faster subsequent builds
RUN mvn dependency:go-offline -B

# Copy all source code
COPY . .

# Build the application, skipping tests to speed up the Docker build
# If you want to run tests during the Docker build, remove -DskipTests
RUN mvn clean install -DskipTests

# --- Runtime Stage ---
FROM openjdk:21-jre-slim

# Create a non-root user and group
RUN addgroup --system springuser && adduser --system --ingroup springuser springuser
USER springuser

# Set the working directory
WORKDIR /app

# Copy the built JAR from the build stage
# Assuming 'scms-api' is your main Spring Boot executable module
# Adjust the path to your main module's target directory and JAR name
COPY --from=build /app/scms-api/target/scms-api-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot application runs on
EXPOSE 8080

# Define the command to run your Spring Boot application
# Ensure application.jar is the name of your executable JAR
ENTRYPOINT ["java", "-jar", "app.jar"]

# --- Best Practices / Considerations ---
# 1. Update JAR Name:
#    The JAR file name `scms-api-0.0.1-SNAPSHOT.jar` is a common default.
#    Verify the exact name of your executable JAR after `mvn clean install`
#    in `scms-api/target/`. If you've configured a different `finalName` in your
#    pom.xml, adjust `scms-api-0.0.1-SNAPSHOT.jar` accordingly.
# 2. Ports:
#    If your Spring Boot application runs on a different port (e.g., 8081),
#    change `EXPOSE 8080` to `EXPOSE <your_port>`.
# 3. Environment Variables:
#    For production, you might want to externalize sensitive configurations
#    like database credentials or encryption keys. You can pass them as
#    environment variables when running the Docker container:
#    `docker run -p 8080:8080 -e ENCRYPTION_KEY="your_actual_secret_key" your-image-name`
#    (Note: Hardcoding keys in Dockerfile or application.properties is NOT secure for production).
# 4. Database Connection:
#    When running with Docker Compose, your `application.properties` or
#    `application.yml` should use the Docker service name for the database host
#    (e.g., `spring.datasource.url=jdbc:postgresql://postgres_db_service_name:5432/yourdb`).
# 5. Health Checks:
#    For production deployments, consider adding `HEALTHCHECK` instructions
#    to your Dockerfile.
# 6. BuildKit:
#    Using `DOCKER_BUILDKIT=1 docker build ...` can improve build performance
#    and introduce features like `--mount=type=cache`.