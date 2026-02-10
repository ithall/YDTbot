FROM eclipse-temurin:17-jdk-alpine

# Устанавливаем глобальный Gradle
RUN apk add --no-cache gradle

WORKDIR /app

COPY . .

# Запускаем глобальный gradle вместо ./gradlew
RUN gradle build --no-daemon

CMD ["java", "-jar", "build/libs/YDTbot-1.0-SNAPSHOT.jar"]
