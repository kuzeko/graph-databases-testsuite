FROM openjdk:13-slim-buster
LABEL authors="Brugnara <mb@disi.unitn.eu>, Lissandrini <ml@disi.unitn.eu>" \
      project="graphbenchmark.com" \
      com.graphbenchmark.version="2.0.0"

RUN apt-get update && \
    apt-get install -y --no-install-recommends numactl && \
    rm -rf /var/lib/apt/lists/*

# Data/indexes directory
RUN mkdir /data
RUN chmod 777 /data

COPY /extra/common-init.sh /entrypoint.sh
RUN chmod 111 /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]  # Will invoke the shell
CMD []                         # Reset
