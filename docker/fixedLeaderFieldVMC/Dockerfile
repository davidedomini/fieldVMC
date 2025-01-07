FROM eclipse-temurin:21
RUN mkdir -p /experiment /data
VOLUME "/data"
ENV CLONING_RANGE=${CLONING_RANGE}
ENV DEFAULT=${DEFAULT}
ENV LEADER_RADIUS=${LEADER_RADIUS}
ENV MAX=${MAX}
ENV MAX_CHILDREN=${MAX_CHILDREN}
ENV MAX_RESOURCE=${MAX_RESOURCE}
ENV MIN=${MIN}
ENV MIN_SPAWN_WAIT=${MIN_SPAWN_WAIT}
ENV RESOURCE_LOWER_BOUND=${RESOURCE_LOWER_BOUND}
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
ARG CLONING_RANGE
ARG DEFAULT
ARG LEADER_RADIUS
ARG MAX
ARG MAX_CHILDREN
ARG MAX_RESOURCE
ARG MIN
ARG MIN_SPAWN_WAIT
ARG RESOURCE_LOWER_BOUND
CMD ./gradlew runFixedLeaderBatch
