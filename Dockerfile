# Use lightweight JDK image to run the jar
#FROM eclipse-temurin:17-jdk-alpine

FROM public.ecr.aws/amazoncorretto/amazoncorretto:17


WORKDIR /app
# Copy the jar from the target folder
COPY target/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
