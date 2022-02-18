FROM openjdk:17-jre-alpine
VOLUME /tmp
COPY newsfeed-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]