FROM eclipse-temurin:21
RUN mkdir -p /experiment /data
VOLUME "/data"
ENV MAX=${MAX}
ENV MIN=${MIN}
ENV DEFAULT=${DEFAULT}
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
CMD ./gradlew runFixedLeaderBatch
