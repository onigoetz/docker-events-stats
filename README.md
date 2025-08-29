# Docker Event Listener

> This is an experiment and I don't intend to add more features to it

A Java application that monitors Docker events, specifically tracking "pull" and "run" events, and logs image details including name and size.

## Features

- Connects to the local Docker daemon
- Monitors Docker events in real-time
- Filters for image pull and container start events
- Logs image details (name and size) to both console and a log file
- Runs continuously in the background

## Prerequisites

- JDK 11 or higher
- Maven
- Docker installed and running on the host machine
- Docker daemon socket accessible (typically at `/var/run/docker.sock`)

## Building the Application

```bash
mvn clean package
```

This will create a JAR file with all dependencies included at `target/docker-event-listener-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Running the Application

```bash
java -jar target/docker-event-listener-*-jar-with-dependencies.jar "http://localhost:8086" admin-token my-org my-bucket
```

## Log Output

The application generates logs in two places:

1. Console output for the text version
2. InfluxDB / Grafana to check the data

Example log entry:
```
[main] INFO ch.onigoetz.dockerstats.DockerEventListener - Starting Docker event listener...
[main] INFO ch.onigoetz.dockerstats.DockerEventListener - Monitoring for interesting events...
[docker-java-stream--420933693] INFO ch.onigoetz.dockerstats.EventResultCallback - Event listener started
[docker-java-stream--420933693] INFO ch.onigoetz.dockerstats.EventRecorder - PULL: alpine:3.19.7, 1746802329, HIT, 0.0 MB Downloaded
[docker-java-stream--420933693] INFO ch.onigoetz.dockerstats.EventRecorder - PULL: alpine:3.19.6, 1746802341, MISS, 7.378216743469238 MB Downloaded
```

## Note on Docker Access

The application requires access to the Docker daemon.
On Linux, ensure your user has permissions to access the Docker socket or run the application with elevated privileges.
On Windows and macOS, ensure Docker Desktop is running.
