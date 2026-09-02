FROM eclipse-temurin:26-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean bootJar -x test --no-daemon
RUN jar tf build/libs/*.jar | grep "BOOT-INF/classes/static" || true

FROM eclipse-temurin:26-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]