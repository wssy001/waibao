FROM adoptopenjdk/openjdk8

RUN mkdir -p /usr/waibao-gateway
EXPOSE 9527

ENV JAVA_OPTS="\
-Djava.security.egd=file:/dev/./urandom\
"

ADD target/*.jar /usr/waibao-gateway/waibao-gateway.jar
WORKDIR   /usr/waibao-gateway
RUN bash -c 'touch /usr/waibao-gateway/waibao-gateway.jar'
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone
ENTRYPOINT ["java","-server","-Dreactor.netty.pool.leasingStrategy=lifo","-jar","/usr/waibao-gateway/waibao-gateway.jar"]