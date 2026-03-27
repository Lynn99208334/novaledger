# ---- Build Stage ----
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# 安裝 Node.js 18
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn

COPY pom.xml .
COPY health/pom.xml health/
COPY common/pom.xml common/
COPY auth/pom.xml auth/
COPY finance-core/pom.xml finance-core/
COPY application/pom.xml application/

RUN ./mvnw dependency:go-offline -B

COPY health/src health/src
COPY common/src common/src
COPY auth/src auth/src
COPY application/src application/src

RUN ./mvnw package -DskipTests -Dskip.npm=true -B

# ---- Run Stage ----
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/application/target/novaledger.jar app.jar

EXPOSE 8111

ENTRYPOINT ["java", "-jar", "app.jar"]