FROM adoptopenjdk/openjdk8

RUN mkdir -p /usr/waibao-payment
EXPOSE 8005

ENV JAVA_OPTS="\
-Djava.security.egd=file:/dev/./urandom\
"

ADD target/*.jar /usr/waibao-payment/waibao-payment.jar
WORKDIR   /usr/waibao-payment
RUN bash -c 'touch /usr/waibao-payment/waibao-payment.jar'
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone
ENTRYPOINT ["java","-server","-Xmx2048m","-Xms2048m","-jar","/usr/waibao-payment/waibao-payment.jar"]