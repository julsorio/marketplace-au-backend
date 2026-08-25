# syntax=docker/dockerfile:1

# ---- Etapa de compilación ----
# Compila el jar con el wrapper de Maven del propio proyecto (mvnw), así se usa exactamente
# la misma versión de Maven que en desarrollo sin depender de que la imagen la tenga instalada.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copiamos primero solo lo necesario para resolver dependencias. Mientras pom.xml no cambie,
# Docker reutiliza la caché de esta capa (incluida la descarga de todo el árbol de Maven) en
# los siguientes builds, aunque cambie el código fuente.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# A partir de aquí es donde más cambia entre builds.
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ---- Etapa de ejecución ----
# JRE en vez de JDK: no hace falta compilador ni herramientas de build para correr un jar ya
# compilado, y la imagen final queda bastante más pequeña (y con menos superficie de ataque).
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Usuario no root: si el proceso de la aplicación se ve comprometido, que no tenga privilegios
# de root dentro del contenedor.
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

# Perfil "main" = configuración de producción (context path /api, ver application-main.properties).
#
# IMPORTANTE — application-main.properties NO incluye jwt.secret, jwt.expiration,
# spring.mongodb.uri ni cors.allowed-origins, y eso es intencional: son secretos o config que
# cambia por entorno y nunca deberían quedar horneados dentro de la imagen. Hay que pasarlos
# como variables de entorno al arrancar el contenedor (Spring Boot las mapea automáticamente
# por "relaxed binding": JWT_SECRET -> jwt.secret, JWT_EXPIRATION -> jwt.expiration,
# SPRING_MONGODB_URI -> spring.mongodb.uri, CORS_ALLOWED_ORIGINS -> cors.allowed-origins).
# Sin ellas el arranque falla — es preferible a arrancar con un secreto por defecto o
# apuntando por accidente a un Mongo equivocado. Ver docker-compose.yml para un ejemplo de
# cómo pasarlas en local; en AWS/GCP irían en el gestor de secretos de la plataforma
# (ECS task definition / Cloud Run env vars + Secret Manager), no en texto plano en el repo.
ENV SPRING_PROFILES_ACTIVE=main

EXPOSE 8002

# No hay Actuator instalado todavía, así que no añadimos aquí un HEALTHCHECK que dependa de
# herramientas (curl/wget) que esta imagen base no garantiza tener. Recomendado antes de
# producción: añadir spring-boot-starter-actuator y usar /api/actuator/health como healthcheck,
# tanto aquí como en el load balancer/target group de ECS o en el probe de Cloud Run.
ENTRYPOINT ["java", "-jar", "app.jar"]
