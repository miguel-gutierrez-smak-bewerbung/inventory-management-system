FROM eclipse-temurin:21-jre

LABEL org.opencontainers.image.title="Product Service" \
      org.opencontainers.image.description="Inventory Product Service" \
      org.opencontainers.image.licenses="Apache-2.0"

WORKDIR /app
COPY ./target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]