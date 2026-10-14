# Build stage: compile and package with the Maven wrapper, then split the jar into layers.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN ./mvnw --batch-mode --quiet dependency:go-offline

COPY src src
RUN ./mvnw --batch-mode --quiet -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true package \
    && java -Djarmode=tools -jar target/daily-trends-java-*.jar extract --layers --launcher \
        --destination target/extracted

# Runtime stage: JRE only, non-root user. Dependencies come first so code changes reuse their layers.
FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app

COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

USER app
ENV SPRING_PROFILES_ACTIVE=production
EXPOSE 5000
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
