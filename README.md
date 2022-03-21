# Climbd cycling news

![Docker Build](https://github.com/mithandir/cyclingnews/actions/workflows/docker-publish.yml/badge.svg)

Running at: https://news.qfotografie.de

<img src="img.png" width="500" >

This is a "Hackernews" clone for cycling related content. Content entries are automatically created using RSS feeds.

The project uses Vaadin to create the frontend. The backend is using Spring Boot with a reactive MongoDB backend.

## Setup

Look into the `src/main/ressources` folder and edit the `example-application.yaml`. After modifying the values rename the file to `application.yaml`.

Set the maven profile to "dev" for development and "prod" for releases.

You can start the application like a normal spring boot application. The frontend should be available at http://localhost:8080.
