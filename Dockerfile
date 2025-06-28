# --- Build Stage ---
# Use a Maven image with OpenJDK 17 on Alpine Linux for building.
# This combination is usually very lean and widely available.
FROM maven:3.9.6-openjdk-17-alpine AS build

# Set the working directory inside the container for the build stage.
WORKDIR /app

# Copy the parent pom.xml first to leverage Docker's build cache.
COPY pom.xml .

# Copy individual module pom.xml files.
COPY scms-api/pom.xml scms-api/
COPY scms-core/pom.xml scms-core/
COPY scms-infrastructure/pom.xml scms-infrastructure/

# Download all project dependencies.
RUN mvn dependency:go-offline -B

# Copy all source code into the working directory.
COPY . .

# Build the multi-module application.
RUN mvn clean install -DskipTests

# --- Runtime Stage ---
# Start a new, smaller image for running the application.
# `openjdk:17-jre-alpine` provides only the Java 17 Runtime Environment on Alpine.
FROM openjdk:17-jre-alpine AS runtime

# Create a dedicated non-root user and group for security best practices.
# Alpine uses `addgroup` and `adduser` differently than Debian/Ubuntu.
RUN addgroup -S springuser && adduser -S springuser -G springuser
USER springuser

# Set the working directory inside the container for the runtime.
WORKDIR /app

# Copy the built executable JAR from the 'build' stage into the runtime image.
# IMPORTANT: Double-check the exact JAR filename produced by Maven.
COPY --from=build /app/scms-api/target/scms-api-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot application listens on.
EXPOSE 8080

# Define the command to run your Spring Boot application when the container starts.
ENTRYPOINT ["java", "-jar", "app.jar"]