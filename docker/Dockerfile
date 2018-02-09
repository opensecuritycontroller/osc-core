FROM osc-base-image:osc-base

MAINTAINER Arvind Nadendla <arvindn05@gmail.com>

#RUN source /etc/profile.d/proxy.sh && \
RUN git clone https://github.com/opensecuritycontroller/osc-core.git && \
cd osc-core && \
mvn clean install && \
cd target && \
unzip serverUpgradeBundle.zip && \
cp -r opt / && \
cd $HOME && \
yum remove -y ant && \
yum remove -y jdk && \
rm -rf /usr/local/apache-maven* && \
rm -rf osc-core

EXPOSE 8090 443

WORKDIR /opt/vmidc/bin/

RUN chmod +x vmidc.sh

# Run OSC in console mode, otherwise container exists immediately
CMD ["/opt/vmidc/bin/vmidc.sh", "--console", "--start"]
