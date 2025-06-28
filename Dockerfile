# --- Build Stage ---
FROM maven:3-openjdk-21 AS build
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