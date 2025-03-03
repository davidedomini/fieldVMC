FROM eclipse-temurin:21
RUN mkdir -p /experiment /data
VOLUME "angela.cortecchia-volume"
ENV DATA_DIR=/data
WORKDIR /experiment
ENV OWNER=1000:1000
ENV GRADLE_TASK=runCuttingClassicVMCBatch
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
CMD ./gradlew $GRADLE_TASK
