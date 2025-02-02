FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . /app
RUN mvn clean package -Pprod -DskipTests

FROM openjdk:17.0.1-jdk-slim
COPY --from=build /app/target/furkankarakus-0.0.1-SNAPSHOT.jar furkankarakus.jar
EXPOSE 8443
ENTRYPOINT ["java","-jar","furkankarakus.jar"]

FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . /app
RUN mvn clean package -Pprod -DskipTests
# Create necessary directories with proper permissions
RUN mkdir -p /app/invoices /app/uploads
RUN chown -R javauser:javauser /app

# Declare environment variables
ENV DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=renovatipoint \
    SPRING_PROFILES_ACTIVE=prod

FROM openjdk:17.0.1-jdk-slim





