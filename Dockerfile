# Render 배포용 — Render가 이 파일로 빌드·실행한다 (로컬에 Docker 없어도 됨)
# 프론트엔드: 로컬에서 빌드한 산출물을 src/main/resources/static 에 커밋해 두면
# Spring 이 배포 URL 루트(/)에서 화면을, /api/** 에서 API를 함께 서빙한다.
# (마감 대응 — 프론트 수정 시 frontend/ 에서 npm run build 후 static 갱신, README 참고)

# 1단계: Gradle 빌드
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
# 의존성만 먼저 받아 레이어 캐시 (실패해도 다음 단계에서 다시 받으므로 무시)
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# 2단계: 실행 (JRE만 — 이미지 축소)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# Render 무료 티어 RAM 512MB → 힙 상한 필수 (docs/05)
ENV JAVA_OPTS="-Xmx384m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
