FROM eclipse-temurin:8-jdk AS builder

WORKDIR /build
COPY . .
RUN ./gradlew compileTestJava --no-daemon -x lint

FROM eclipse-temurin:8-jre

WORKDIR /app
COPY --from=builder /build /app

ENTRYPOINT ["./gradlew"]
CMD ["smokeTest", "--no-daemon"]
