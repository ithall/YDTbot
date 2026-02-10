FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .

RUN ./gradlew build --no-daemon

CMD ["java", "-jar", "build/libs/youtube-downloader-telegram-bot-1.0-SNAPSHOT.jar"]
