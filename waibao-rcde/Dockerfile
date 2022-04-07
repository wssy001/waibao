FROM adoptopenjdk/openjdk8

RUN mkdir -p /usr/waibao-rcde
EXPOSE 33382

ENV JAVA_OPTS="\
-Djava.security.egd=file:/dev/./urandom\
"

ADD target/*.jar /usr/waibao-rcde/waibao-rcde.jar
WORKDIR   /usr/waibao-rcde
RUN bash -c 'touch /usr/waibao-rcde/waibao-rcde.jar'
ENTRYPOINT ["java","-server","-Xmx2048m","-Xms2048m","-jar","/usr/waibao-rcde/waibao-rcde.jar"]