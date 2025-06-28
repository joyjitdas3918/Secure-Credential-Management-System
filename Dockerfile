# --- Build Stage ---
FROM maven:3-openjdk-21 AS build
WORKDIR /app

# Copy the parent pom.xml and download dependencies
COPY pom.xml .
COPY scms-api/pom.xml scms-api/
COPY scms-core/pom.xml scms-core/
COPY scms-infrastructure/pom.xml scms-infrastructure/

RUN mvn dependency:go-offline -B

COPY . .

RUN mvn clean install -DskipTests

# --- Runtime Stage ---
FROM openjdk:21-jre-slim-bullseye AS runtime # <--- CHANGE THIS LINE
# Or, if that still doesn't work, try: FROM openjdk:21-jre AS runtime

# Create a non-root user and group
RUN addgroup --system springuser && adduser --system --ingroup springuser springuser
USER springuser

# Set the working directory
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/scms-api/target/scms-api-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot application runs on
EXPOSE 8080

# Define the command to run your Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]