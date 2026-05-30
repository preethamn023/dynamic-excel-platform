# Backend Dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine as build
WORKDIR /app
COPY pom.xml .
# Download dependencies offline to cache them
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/dynamic-excel-platform-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
