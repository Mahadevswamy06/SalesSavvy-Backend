FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src/ src/

RUN ./mvnw clean package -DskipTests

EXPOSE 9090

CMD ["sh", "-c", "java -jar target/*.jar"]
