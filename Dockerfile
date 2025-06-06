# syntax=docker/dockerfile:experimental
FROM maven:3-eclipse-temurin-24 as builder

LABEL NAME="newsfeed-build"
LABEL VERSION=1.0.0
LABEL MAINTAINER=mithandir@gmail.com

RUN mkdir /opt/src
COPY / /opt/src/newsfeed/

WORKDIR /opt/src/newsfeed
RUN --mount=type=cache,target=/root/.m2 mvn -q install -DskipTests=true -P production && cp target/*.jar /opt/app.jar

#------------------------------------------------

FROM eclipse-temurin:24-jre-alpine
VOLUME /tmp
LABEL NAME="climbd-newsfeed"
LABEL VERSION=1.0.0
LABEL MAINTAINER=mithandir@gmail.com

RUN addgroup -S newsfeed && adduser -S newsfeed -G newsfeed
USER newsfeed

COPY --from=builder /opt/app.jar /opt/app.jar
ENTRYPOINT ["java","--enable-preview", "-jar", "/opt/app.jar"]
