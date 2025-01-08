FROM eclipse-temurin:21
RUN mkdir -p /experiment /data
VOLUME "/data"
ENV CLONING_RANGE=${CLONING_RANGE}
ENV MAX_CHILDREN=${MAX_CHILDREN}
ENV MAX_RESOURCE=${MAX_RESOURCE}
ENV MAX_SUCCESS=${MAX_SUCCESS}
ENV MIN_SPAN_WAIT=${MIN_SPAN_WAIT}
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
ARG MAX_CHILDREN
ARG MAX_RESOURCE
ARG MAX_SUCCESS
ARG MIN_SPAN_WAIT
ARG RESOURCE_LOWER_BOUND
CMD ./gradlew runClassicVMCBatch
