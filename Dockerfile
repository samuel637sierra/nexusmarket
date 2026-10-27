FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/target/NexusMarket-0.0.1-SNAPSHOT.jar /app/nexusmarket.jar
RUN mkdir -p /var/data

EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/nexusmarket.jar"]
