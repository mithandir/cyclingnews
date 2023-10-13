# syntax=docker/dockerfile:experimental
FROM maven:3-sapmachine-21 AS build-env

LABEL NAME="newsfeed-build"
LABEL VERSION=1.0.0
LABEL MAINTAINER=mithandir@gmail.com

RUN mkdir /opt/src
COPY / /opt/src/newsfeed/

WORKDIR /opt/src/newsfeed
RUN --mount=type=cache,target=/root/.m2 mvn -q install -DskipTests=true -P production
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)


FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
LABEL NAME="climbd-newsfeed"
LABEL VERSION=1.0.0
LABEL MAINTAINER=mithandir@gmail.com

ARG DEPENDENCY=/opt/src/newsfeed/target/dependency

RUN addgroup -S newsfeed && adduser -S newsfeed -G newsfeed
USER newsfeed

COPY --from=build-env ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build-env ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build-env ${DEPENDENCY}/BOOT-INF/classes /app

ENTRYPOINT ["java","--enable-preview","-cp","app:app/lib/*","ch.climbd.newsfeed.NewsfeedApplication"]
