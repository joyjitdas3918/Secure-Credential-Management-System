# --- Build Stage ---
# Use a Maven image with OpenJDK 21 for building the application.
# `maven:3.9.6-sapmachine-jdk-21` is a robust choice.
FROM maven:3.9.6-sapmachine-jdk-21 AS build
WORKDIR /app

# Copy the parent pom.xml first to leverage Docker's build cache.
COPY pom.xml .
# Copy individual module pom.xml files.
COPY scms-api/pom.xml scms-api/
COPY scms-core/pom.xml scms-core/
COPY scms-infrastructure/pom.xml scms-infrastructure/

# Download all project dependencies.
RUN mvn dependency:go-offline -B

# Copy all source code.
COPY . .

# Build the multi-module application.
RUN mvn clean install -DskipTests

# --- Runtime Stage ---
# Start a new, smaller image for running the application.
# `openjdk:21-jre-slim-bullseye` is generally a good choice.
FROM openjdk:17-jdk-alpine

# Create a dedicated non-root user and group for security best practices.
RUN addgroup --system springuser && adduser --system --ingroup springuser springuser
USER springuser

# Set the working directory inside the container for the runtime.
WORKDIR /app

# Copy the built executable JAR.
COPY --from=build /app/scms-api/target/scms-api-0001-SNAPSHOT.jar app.jar

# Expose the port.
EXPOSE 8080

# Define the command to run.
ENTRYPOINT ["java", "-jar", "app.jar"]