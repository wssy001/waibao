FROM adoptopenjdk/openjdk8

RUN mkdir -p /usr/waibao-user
EXPOSE 8001

ENV JAVA_OPTS="\
-Djava.security.egd=file:/dev/./urandom\
"

ADD target/*.jar /usr/waibao-user/waibao-user.jar
WORKDIR   /usr/waibao-user
RUN bash -c 'touch /usr/waibao-user/waibao-user.jar'
ENTRYPOINT ["java","-server","-Xmx2048m","-Xms2048m","-jar","/usr/waibao-user/waibao-user.jar"]