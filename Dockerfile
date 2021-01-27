FROM registry.access.redhat.com/ubi8/openjdk-11

ENTRYPOINT ["java", "-jar", "/usr/share/operator/operator.jar"]

ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/operator/operator.jar

