FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew

# Resolve dependencies before the source is copied, so editing a .java file does
# not re-resolve the whole graph. The cache mount persists ~/.gradle between
# builds: without it every rebuild re-downloads the full dependency set from
# Maven Central, and a single TLS hiccup fails the image build outright.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null 2>&1 || true

COPY src src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
