# =============================================================================
# 멀티스테이지 Dockerfile — 프론트(React)까지 포함된 단일 Spring Boot jar 를 빌드/실행.
#
#   build 스테이지: JDK 21 + Node. gradle bootJar 가 npm build 를 돌려 frontend/dist 를
#     jar 안 static 으로 넣는다(build.gradle). 통합테스트(Testcontainers)는 도커가 필요하니
#     빌드 단계에서 제외한다(-x test).
#   runtime 스테이지: JRE 21 만. 완성된 jar 하나만 실행한다.
# =============================================================================

# --- Node 바이너리를 JDK 이미지로 가져오기 위한 소스 스테이지 ---
FROM node:22-bookworm-slim AS node

# --- build: jar 빌드 ---
FROM eclipse-temurin:21-jdk AS build
# JDK 이미지에 Node/npm 을 얹는다.
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -sf /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
 && ln -sf /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx

WORKDIR /workspace
COPY . .
RUN chmod +x gradlew \
 && ./gradlew --no-daemon clean bootJar -x test \
 && cp "$(find build/libs -name '*.jar' ! -name '*-plain.jar' | head -1)" /workspace/app.jar

# --- runtime: 실행 ---
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8080
# 컨테이너에 실 메모리 한도가 있으면 JVM 이 그에 맞춰 힙을 잡도록.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
