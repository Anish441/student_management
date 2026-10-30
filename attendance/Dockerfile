FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /build/target/attendance.jar app.jar
RUN mkdir -p /app/data && chown -R app:app /app

USER app
EXPOSE 8081
VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
