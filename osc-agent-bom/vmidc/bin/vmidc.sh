#!/bin/sh
cd `dirname $0`

export JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Xmx64m"
# DEBUG : Uncomment below line for opening debug port.
# /usr/local/sbin/iptables -I INPUT -i eth0 -p tcp --dport 8787 -j ACCEPT
# export JAVA_OPTS="$JAVA_OPTS -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"

../jre/bin/java $JAVA_OPTS -jar vmiDCAgent.jar $*