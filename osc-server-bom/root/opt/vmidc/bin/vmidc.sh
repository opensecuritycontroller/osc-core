#!/bin/sh

export LD_LIBRARY_PATH=/opt/vmidc/bin

default_java_opts="
    -Djava.security.egd=file:/dev/./urandom
    -server
    -Xms1024m
    -Xmx4096m
"

# if JAVA_OPTS is not set, then set it to $default_java_opts
: ${JAVA_OPTS:=$default_java_opts}

# Uncomment below line to enable remote debugging.
#JAVA_OPTS="$JAVA_OPTS -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"

# Uncomment below line to enable remote resource monitoring.
#JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=55555 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=`hostname -I`"

PATH=${JAVA_HOME:-../jre}/bin:$PATH

case "$1" in
--console)
    shift
    java -jar osc-control.jar --check || exit 2
    exec java $JAVA_OPTS -cp . aQute.launcher.pre.EmbeddedLauncher "$@"
    ;;
*)
    exec java -jar osc-control.jar "$@"
    ;;
esac

#end
