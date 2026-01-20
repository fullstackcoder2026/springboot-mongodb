FROM maven:3.9.6-eclipse-temurin-17
WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "target/app.jar"]
