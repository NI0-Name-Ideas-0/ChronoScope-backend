# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package the application
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:25-jre
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod

# Copy the built jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (default for Spring Boot is 8080)
EXPOSE 9020

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
