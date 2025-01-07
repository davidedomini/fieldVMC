FROM eclipse-temurin:21
RUN mkdir -p /experiment /data
VOLUME "/data"
ENV MAX=${MAX}
ENV MIN=${MIN}
ENV DEFAULT=${DEFAULT}
ENV RADIUS=${RADIUS}
ENV MAX_RESOURCE=${MAX_RESOURCE}
ENV RESOURCE_LOWER_BOUND=${RESOURCE_LOWER_BOUND}
ENV MAX_CHILDREN=${MAX_CHILDREN}
ENV MIN_SPAN_WAIT=${MIN_SPAN_WAIT}
ENV CLONING_RANGE=${CLONING_RANGE}
WORKDIR /experiment
COPY effects effects
COPY gradle gradle
COPY src src
COPY docker docker
COPY *.kts ./
COPY *.properties ./
COPY gradlew* ./
RUN sed -i '/alias(libs.plugins.gitSemVer)/d' build.gradle.kts
RUN sed -i '/alias(libs.plugins.multiJvmTesting)/d' build.gradle.kts
RUN sed -i '/multiJvm {/,/}/d' build.gradle.kts
ARG MAX
ARG MIN
ARG DEFAULT
ARG RADIUS
ARG MAX_RESOURCE
ARG RESOURCE_LOWER_BOUND
ARG MAX_CHILDREN
ARG MIN_SPAN_WAIT
ARG CLONING_RANGE
CMD ./gradlew runFixedLeaderBatch


# RESOURCE_LOWER_BOUND=1.0
MIN=0
RADIUS=100.0
MAX=20
CLONING_RANGE=5
MAX_RESOURCE=1000.0
MIN_SPAN_WAIT=40.0
DEFAULT=0
MAX_CHILDREN=5 ./gradlew runFixedLeaderGraphic