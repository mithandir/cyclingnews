# syntax=docker/dockerfile:experimental
FROM maven:3-eclipse-temurin-21 as builder

LABEL NAME="newsfeed-build"
LABEL VERSION=1.0.0
LABEL MAINTAINER=mithandir@gmail.com

RUN mkdir /opt/src
COPY / /opt/src/newsfeed/

WORKDIR /opt/src/newsfeed
#RUN --mount=type=cache,target=/root/.m2 mvn -q install -DskipTests=true -P production
RUN mvn -q install -DskipTests=true -P production
RUN java -Djarmode=layertools -jar /opt/src/newsfeed/target/newsfeed-0.0.1-SNAPSHOT.jar extract

FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
LABEL NAME="climbd-newsfeed"
LABEL VERSION=1.0.0
LABEL MAINTAINER=mithandir@gmail.com

RUN addgroup -S newsfeed && adduser -S newsfeed -G newsfeed
USER newsfeed

ARG DEPENDENCY=/opt/src/newsfeed

COPY --from=builder ${DEPENDENCY}/dependencies/ ./
COPY --from=builder ${DEPENDENCY}/spring-boot-loader/ ./
COPY --from=builder ${DEPENDENCY}/snapshot-dependencies/ ./
COPY --from=builder ${DEPENDENCY}/application/ ./

ENTRYPOINT ["java","--enable-preview", "org.springframework.boot.loader.launch.JarLauncher"]
