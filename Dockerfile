FROM java:8-alpine

RUN apk add --no-cache bash

WORKDIR /usr/src/app
ADD api/build/install/api/ /usr/src/app

EXPOSE 50051

CMD ["bin/GcTool-Server", "-p", "50051"]
