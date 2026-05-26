# Build (monorepo: build context = thư mục BE)
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/flourish-travel-backend-1.0.0-SNAPSHOT.jar app.jar
# DigitalOcean PostgreSQL: application-cloud.yml (profile cloud).
# ECS đôi khi set SPRING_PROFILES_ACTIVE rỗng → ghi đè ENV; dùng ${VAR:-cloud} để luôn có profile khi trống.
# Bắt buộc DB_PASSWORD khi profile cloud. Local H2: docker run -e SPRING_PROFILES_ACTIVE=dev ...
ENV SPRING_PROFILES_ACTIVE=cloud
EXPOSE 8080
# TieredStopAtLevel=1: startup nhanh hơn trên container nhỏ (DO App Platform)
ENTRYPOINT ["sh", "-c", "exec java -XX:+UseContainerSupport -XX:TieredStopAtLevel=1 -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-cloud}"]
