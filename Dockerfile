# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory in the container
WORKDIR /app

# Copy the application JAR file to the working directory
#COPY /src/main/resources/Secrets/service-key.json /app/src/main/resources/Secrets/service-key.json
COPY target/*.jar /app/app.jar

# Update package lists and install nginx + curl
RUN apt-get update && \
    apt-get install -y nginx curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy your nginx configuration to the container
COPY nginx.conf /etc/nginx/nginx.conf

# Expose the port on which the application will run
EXPOSE 8080

# Run the JAR file and Nginx together
CMD ["sh", "-c", "java -jar /app/app.jar & nginx -g 'daemon off;'"]
