# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY core/pom.xml core/pom.xml
COPY application/pom.xml application/pom.xml
COPY infrastructure/pom.xml infrastructure/pom.xml
RUN ./mvnw -B -q dependency:go-offline

COPY core core
COPY application application
COPY infrastructure infrastructure
RUN ./mvnw -B -q -DskipTests package \
    && cp infrastructure/target/stock-pilot-infrastructure-*.jar /workspace/app.jar

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
RUN groupadd --system stockpilot && useradd --system --gid stockpilot --no-create-home stockpilot
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER stockpilot
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
