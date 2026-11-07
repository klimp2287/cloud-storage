FROM ubuntu:latest
LABEL authors="klimp"

ENTRYPOINT ["top", "-b"]