FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY event-service/pom.xml event-service/pom.xml
COPY inscription-service/pom.xml inscription-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY feedback-service/pom.xml feedback-service/pom.xml
RUN mvn -pl user-service -am dependency:go-offline -B
COPY user-service/src user-service/src
RUN mvn -pl user-service -am package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/user-service/target/*.jar app.jar
EXPOSE 9003
ENTRYPOINT ["java", "-jar", "app.jar"]
