# Render 배포용 — Render가 이 파일로 빌드·실행한다 (로컬에 Docker 없어도 됨)
# 구조: 프론트(Vite) 빌드 → 산출물을 Spring static/ 에 넣어 한 서버가 화면+API를 모두 서빙
#       → 배포 URL 하나(https://…onrender.com)로 제출 요건 충족

# 0단계: 프론트엔드 빌드 (alpine 금지 — rollup 네이티브 모듈이 musl용을 못 찾는 이슈)
FROM node:20 AS fe
WORKDIR /fe
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend ./
RUN npm run build

# 1단계: Gradle 빌드 (프론트 산출물을 static 리소스로 포함)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
# 의존성만 먼저 받아 레이어 캐시 (실패해도 다음 단계에서 다시 받으므로 무시)
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src ./src
COPY --from=fe /fe/dist ./src/main/resources/static
RUN ./gradlew --no-daemon clean bootJar

# 2단계: 실행 (JRE만 — 이미지 축소)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# Render 무료 티어 RAM 512MB → 힙 상한 필수 (docs/05)
ENV JAVA_OPTS="-Xmx384m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
