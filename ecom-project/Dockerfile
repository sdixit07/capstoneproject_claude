# --- Build stage ---
FROM maven:3.9.8-eclipse-temurin-21 as build
WORKDIR /app

# Copy pom first to leverage layer caching
COPY pom.xml ./
COPY src ./src

# Skip tests in image build (run tests in CI pipeline)
RUN mvn -q -D skipTests package

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-jammy as runtime
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=default
EXPOSE 8080

COPY --from=build /app/target/*.jar /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
