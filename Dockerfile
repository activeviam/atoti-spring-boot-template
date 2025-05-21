FROM eclipse-temurin:21-alpine
RUN mkdir /opt/app
RUN apk --no-cache add bash curl
ARG JAR_FILE=target/atoti-spring-boot-template.jar
COPY ${JAR_FILE}  /opt/app/app.jar
CMD ["java", "-jar", "/opt/app/app.jar"]