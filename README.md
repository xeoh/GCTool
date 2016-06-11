GCTool
======

GimChiTool helps to analyze logs of the CMS garbage collector.

Project Structure
-----------------

- common : contains shared models for other subprojects.
- parser : parses the text logs to the data models.
- analyzer : analyzes the data and make useful stats.
- api : API server to receive the logs.
- cli-client : A CLI-based client for uploading the logs.

Usage
-----

__Build & Start the server__

```bash
# On the build node.
# Build the docker image and archive it, because we do not have a docker image registry.
$ ./gradlew clean installDist
$ docker build -t gctool/gctool-server .
$ docker save gctool/gctool-server > gctool-server.tar
$ scp -i <private key file> gctool-server.tar <server username>@<server host>:~

# On the server node.
$ sudo docker load < gctool-server.tar
$ sudo docker-compose up --force-recreate -d
```

__Upload the log and retrieve the analyzed information__

```bash
$ java -jar cli-client/build/libs/cli-client-0.1.0-SNAPSHOT.jar -f <log file location> -h <host> -p <port>
$ java -jar cli-client/build/libs/cli-client-0.1.0-SNAPSHOT.jar -rd <file id> -h <host> -p <port>
```

Server Information
------------------

Alpha : 52.10.179.90:50051

