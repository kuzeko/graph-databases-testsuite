FROM  openjdk:8
LABEL authors="Brugnara <mb@disi.unitn.eu>, Matteo Lissandrini <ml@disi.unitn.eu>"

# RUN gpg --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys CD8CB0F1E0AD5B52E93F41E7EA93F5E56E751E9B B97B0AFCAA1A47F044F244A07FCC7D46ACCC4CF8 7FCC7D46ACCC4CF8

ENV GREMLIN3_TAG 3.2.9
ENV GREMLIN3_HOME /opt/gremlin
ENV PATH /opt/gremlin/bin:$PATH


RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
        build-essential \
        libstdc++6 \
        libgoogle-perftools4 \
        ca-certificates \
        pwgen \
        wget \
        openssl \
        golang \
        curl \
        bash \
        maven \
        ant \
        git-core \
        locales \
        wget \
        sudo


# --> fixing missing libssl1.0.0
ENV LIBSSL_DEB libssl1.0.0_1.0.1t-1+deb7u4_amd64.deb
# ENV LIBSSL_URL http://security.debian.org/debian-security/pool/updates/main/o/openssl/${LIBSSL_DEB}
COPY extra/pkg/$LIBSSL_DEB .
RUN dpkg -i ${LIBSSL_DEB} && \
    rm -f ${LIBSSL_DEB}

# Postgresql, heavily inspired (copied) from:
# https://github.com/docker-library/postgres/blob/54053ad27ac099abff3d4964bf7460fb9c541d5d/9.6/Dockerfile
RUN groupadd -r postgres --gid=999 && useradd -r -g postgres --uid=999 postgres

# make the "en_US.UTF-8" locale so postgres will be utf-8 enabled by default
RUN localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
ENV LANG en_US.utf8

# RUN set -ex; \
#     key='B97B0AFCAA1A47F044F244A07FCC7D46ACCC4CF8'; \
#     gpg --export "$key" > /etc/apt/trusted.gpg.d/postgres.gpg; \
#     apt-key list

ENV PG_MAJOR 9.6
#ENV PG_VERSION 9.6.3-1.pgdg80+1

RUN echo 'deb http://apt.postgresql.org/pub/repos/apt/ jessie-pgdg main' $PG_MAJOR > /etc/apt/sources.list.d/pgdg.list

RUN apt-get update \

    && apt-get install -y --allow-unauthenticated postgresql-common \
    && sed -ri 's/#(create_main_cluster) .*$/\1 = false/' /etc/postgresql-common/createcluster.conf \
    && apt-get install -y --allow-unauthenticated \
        postgresql-$PG_MAJOR \
        postgresql-contrib-$PG_MAJOR \
    && rm -rf /var/lib/apt/lists/*


# make the sample config easier to munge (and "correct by default")
RUN mv -v /usr/share/postgresql/$PG_MAJOR/postgresql.conf.sample /usr/share/postgresql/
RUN cp /usr/share/postgresql/postgresql.conf.sample /tmp/postgresql.conf.sample

RUN git clone https://github.com/andreif/pgtune.git  --branch allthethings --single-branch pgtune \
    && pgtune/pgtune --type=Web --version=${PG_MAJOR}  -i /usr/share/postgresql/postgresql.conf.sample -o /tmp/postgresql.conf.sample.tuned \
    && mv /tmp/postgresql.conf.sample.tuned  /usr/share/postgresql/postgresql.conf.sample \
    && sed -ri "s!^#?(listen_addresses)\s*=\s*\S+.*!\1 = '*'!" /usr/share/postgresql/postgresql.conf.sample \
    && sed -i "s/#max_locks_per_transaction\ =\ 64/max_locks_per_transaction\ =\ 256/" /usr/share/postgresql/postgresql.conf.sample \
    && ln -sv ../postgresql.conf.sample /usr/share/postgresql/$PG_MAJOR/


RUN mkdir -p /var/run/postgresql \
    && chown -R postgres:postgres /var/run/postgresql \
    && chmod 2777 /var/run/postgresql

ENV PATH /usr/lib/postgresql/$PG_MAJOR/bin:$PATH
ENV PGDATA /var/lib/postgresql/data
RUN mkdir -p "$PGDATA" && chown -R postgres:postgres "$PGDATA" && chmod 777 "$PGDATA" # this 777 will be replaced by 700 at runtime (allows semi-arbitrary "--user" values)

# (We need to commit the data)
# VOLUME /var/lib/postgresql/data


# Gremlin
#RUN curl -L -o /tmp/gremlin.zip \
#    http://mirror.nohup.it/apache/tinkerpop/${GREMLIN3_TAG}/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG}-bin.zip && \
ADD libs/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG}-bin.tgz /opt
RUN ln -s /opt/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG} ${GREMLIN3_HOME}

WORKDIR /tmp
COPY extra/dot_groovy           /root/.groovy
COPY extra/*-pg.groovy          /tmp/

RUN  ${GREMLIN3_HOME}/bin/gremlin.sh -e /tmp/install-pg.groovy && \
     rm -vf ${GREMLIN3_HOME}/lib/groovy-swing-2.4.*.jar && \
     rm -vf ${GREMLIN3_HOME}/lib/groovy-jsr223-2.4.*-indy.jar && \
     rm -vf ${GREMLIN3_HOME}/lib/groovy-xml-2.4.*.jar && \
     ${GREMLIN3_HOME}/bin/gremlin.sh -e /tmp/activate-pg.groovy

COPY init/pg-init.sh /
RUN chmod 777 /pg-init.sh

COPY extra/pg-label-hash.go /pghash.go

ENV INDEX_QUERY_PREFIX='pg-'

# standard port
#EXPOSE 9999

WORKDIR /runtime
CMD ["/pg-init.sh"]
