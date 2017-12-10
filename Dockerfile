FROM java:8-jre
MAINTAINER      Brice Argenson <brice@barracks.io>

EXPOSE          8080

COPY            barracks-*.jar    /app.jar
CMD             ["java", "-jar", "app.jar"]