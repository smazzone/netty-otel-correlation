FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . /app
RUN mvn clean package

FROM eclipse-temurin:17-jdk
# Install nc (Netcat)
RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*


WORKDIR /app
COPY --from=build /app/target/netty-otel-example-1.0-SNAPSHOT.jar /app/app.jar
CMD ["java", "-jar", "app.jar"]
