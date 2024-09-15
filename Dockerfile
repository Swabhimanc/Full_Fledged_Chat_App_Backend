# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the application JAR file to the working directory
#COPY /src/main/resources/Secrets/service-key.json /app/src/main/resources/Secrets/service-key.json
COPY target/*.jar /app/app.jar

# Expose the port on which the application runs
EXPOSE 5000

# Run the JAR file
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
