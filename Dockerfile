FROM eclipse-temurin:17-jre-alpine

EXPOSE 5050

ADD target/netology-cloud-storage-0.0.1-SNAPSHOT.jar diploma.jar

ENTRYPOINT ["java", "-jar", "diploma.jar"]