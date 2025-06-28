# --- Build Stage ---
# Use a Maven image with OpenJDK 21 for building the application.
# `maven:3.9.6-eclipse-temurin-21-jammy` specifies Maven 3.9.6,
# Eclipse Temurin OpenJDK 21, and the Jammy (Ubuntu 22.04) base.
# This tag is often more reliable if general tags are not found.
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build

# Set the working directory inside the container for the build stage.
WORKDIR /app

# Copy the parent pom.xml first to leverage Docker's build cache.
COPY pom.xml .

# Copy individual module pom.xml files.
COPY scms-api/pom.xml scms-api/
COPY scms-core/pom.xml scms-core/
COPY scms-infrastructure/pom.xml scms-infrastructure/

# Download all project dependencies. This is crucial for caching and speeds up subsequent builds.
RUN mvn dependency:go-offline -B

# Copy all source code into the working directory.
COPY . .

# Build the multi-module application.
RUN mvn clean install -DskipTests

# --- Runtime Stage ---
# Start a new, smaller image for running the application.
# `openjdk:21-jre-slim-bullseye` remains a good choice for the runtime JRE.
FROM openjdk:21-jdk-alpine

# Create a dedicated non-root user and group for security best practices.
RUN addgroup --system springuser && adduser --system --ingroup springuser springuser
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