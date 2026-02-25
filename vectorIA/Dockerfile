FROM maven:4.0.0-rc-5-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN apt-get update && apt-get install -y libstdc++6 && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

# INFORMAR EL PUERTO DONDE SE EJECUTA EL CONTENEDOR (INFORMATIVO)
EXPOSE 8083

#COPY target/jwt-0.0.1-SNAPSHOT.jar STC-App.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
