FROM adoptopenjdk/openjdk8

RUN mkdir -p /usr/waibao-order
EXPOSE 33382

ENV JAVA_OPTS="\
-Djava.security.egd=file:/dev/./urandom\
"

ADD target/*.jar /usr/waibao-order/waibao-order.jar
WORKDIR   /usr/waibao-order
RUN bash -c 'touch /usr/waibao-order/waibao-order.jar'
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone
ENTRYPOINT ["java","-server","-Xmx2048m","-Xms2048m","-jar","/usr/waibao-order/waibao-order.jar"]