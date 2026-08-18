FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml ./pom.xml

COPY src ./src

RUN mvn clean compile package -DskipTests=true


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/target/gateway-1.0.0.jar /app/gateway.jar

ENTRYPOINT ["java", "-jar", "gateway.jar"]