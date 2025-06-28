# --- Runtime Stage (assuming JAR is pre-built) ---
# Use a small OpenJDK 21 JRE (Java Runtime Environment) image.
# This image contains only what's necessary to run your Java application,
# making the final Docker image as small as possible.
FROM openjdk:17-jdk-alpine

# Create a non-root user and group for enhanced security.
RUN addgroup --system springuser && adduser --system --ingroup springuser springuser
USER springuser

# Set the working directory inside the container.
WORKDIR /app

# Copy the pre-built executable JAR into the image.
#
# IMPORTANT: This Dockerfile assumes you have ALREADY RUN `mvn clean install`
# in your project's root directory *before* building this Docker image.
#
# The path `scms-api/target/scms-api-0.0.1-SNAPSHOT.jar` is relative to the
# Docker build context (which should be your project's root directory).
#
# Please double-check the exact JAR name that Maven produces in your
# `scms-api/target/` folder (e.g., it might be `scms-api-0.0.1-SNAPSHOT.jar`
# or `scms-api-0001-SNAPSHOT.jar` from a previous output). Adjust if needed.

# Expose the port your Spring Boot application listens on.
EXPOSE 8080

# Define the command to run your Spring Boot application when the container starts.
ENTRYPOINT ["java", "-jar", "app.jar"]