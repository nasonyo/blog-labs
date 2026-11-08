# --- build 스테이지: Maven 으로 jar 를 만든다 ---
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
# 의존성만 먼저 받아 두면 소스만 바뀔 때 빌드 캐시를 재사용한다
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# --- runtime 스테이지: JRE 와 jar 만 담는다 ---
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /build/target/memo-0.0.1.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
