FROM osc-base-image:osc-base

MAINTAINER Arvind Nadendla <arvindn05@gmail.com>

# Copy OSC opt folder to container
COPY opt /opt

EXPOSE 8090 443

WORKDIR /opt/vmidc/bin/

RUN chmod +x vmidc.sh

# Run OSC in console mode, otherwise container exists immediately
CMD ["/opt/vmidc/bin/vmidc.sh", "--console", "--start"]
