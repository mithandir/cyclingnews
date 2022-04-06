FROM maven:3-openjdk-18-slim AS build-env

LABEL NAME="newsfeed-build"
LABEL VERSION=1.0.0
LABEL MAINTAINER=mithandir@gmail.com

RUN mkdir /opt/src
COPY / /opt/src/newsfeed/

WORKDIR /opt/src/newsfeed
RUN mvn -q clean install -DskipTests=true -P production && cp target/*.jar /opt/app.jar

FROM openjdk:18-alpine

LABEL NAME="climbd-newsfeed"
LABEL VERSION=1.0.0
LABEL MAINTAINER=mithandir@gmail.com

COPY --from=build-env /opt/app.jar /opt/app.jar

CMD ["java","-jar","/opt/app.jar"]
