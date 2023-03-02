FROM eclipse-temurin:17-jre-focal

RUN apt-get update && apt-get install -y curl wget unzip

COPY target/api-registry.jar /app/api-registry.jar

WORKDIR /app

ENTRYPOINT ["java", "-jar", "api-registry.jar"]
