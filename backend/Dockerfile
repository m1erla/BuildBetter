# Build stage
FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /app

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17.0.1-jdk-slim

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/furkankarakus-0.0.1-SNAPSHOT.jar /app/app.jar

# Create directories for file storage
RUN mkdir -p /app/invoices /app/uploads

# Expose port (Render.com will set the PORT environment variable)
EXPOSE 8080

# JVM options optimized for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]