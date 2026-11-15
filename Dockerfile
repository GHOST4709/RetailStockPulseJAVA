# Build the executable, dependency-inclusive JAR.
FROM maven:3.10.0-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp clean package

# Run the Java application in a smaller JRE image.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/retail-stock-pulse-1.0.0.jar /app/app.jar

# Vercel Container Runtime sends HTTP traffic to port 80 by default.
# Main.java reads PORT, so the app binds to the expected port in this image.
ENV PORT=80
EXPOSE 80

CMD ["java", "-jar", "/app/app.jar"]
