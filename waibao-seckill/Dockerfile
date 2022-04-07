FROM adoptopenjdk/openjdk8

RUN mkdir -p /usr/waibao-seckill
EXPOSE 33381

ENV JAVA_OPTS="\
-Djava.security.egd=file:/dev/./urandom\
"

ADD target/*.jar /usr/waibao-seckill/waibao-seckill.jar
WORKDIR   /usr/waibao-seckill
RUN bash -c 'touch /usr/waibao-seckill/waibao-seckill.jar'
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone
ENTRYPOINT ["java","-server","-Xmx2048m","-Xms2048m","-jar","/usr/waibao-seckill/waibao-seckill.jar"]