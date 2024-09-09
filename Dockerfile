FROM maven:3.9.9-amazoncorretto-21 AS build

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM amazoncorretto:21.0.4-alpine3.20

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
