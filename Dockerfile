FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/demo2-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", \
    "-Xms256m", "-Xmx512m", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
    "-Dserver.port=${PORT:-8080}", \
    "-Dspring.datasource.url=jdbc:mysql://${DB_HOST:-localhost}:${DB_PORT:-3306}/${DB_NAME:-railway_ticket}?useUnicode=true&characterEncoding=utf-8&useSSL=${DB_SSL:-false}&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true", \
    "-Dspring.datasource.username=${DB_USERNAME:-root}", \
    "-Dspring.datasource.password=${DB_PASSWORD:-}", \
    "-Djwt.secret=${JWT_SECRET:-railway_ticket_secret_key_2026}", \
    "app.jar"]