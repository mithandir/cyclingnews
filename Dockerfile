FROM maven:3-eclipse-temurin-19 AS build-env

LABEL NAME="newsfeed-build"
LABEL VERSION=1.0.0
LABEL MAINTAINER=mithandir@gmail.com

RUN mkdir /opt/src
COPY / /opt/src/newsfeed/

WORKDIR /opt/src/newsfeed
RUN mvn -q clean install -DskipTests=true -P production && cp target/*.jar /opt/app.jar

FROM eclipse-temurin:19-jre

LABEL NAME="climbd-newsfeed"
LABEL VERSION=1.0.0
LABEL MAINTAINER=mithandir@gmail.com

COPY --from=build-env /opt/app.jar /opt/app.jar

RUN apt-get update && apt-get -y upgrade

CMD ["java","--enable-preview","-jar","/opt/app.jar"]
