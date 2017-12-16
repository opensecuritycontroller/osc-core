#!/bin/sh
# Copyright (c) Intel Corporation
# Copyright (c) 2017
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

default_java_opts="
    -Djava.security.egd=file:/dev/./urandom
    -server
    -Xms1024m
    -Xmx4096m
"

# Extend size of ephemeral Diffie-Hellman keys
JAVA_OPTS="$JAVA_OPTS -Djdk.tls.ephemeralDHKeySize=2048"

# if JAVA_OPTS is not set, then set it to $default_java_opts
: ${JAVA_OPTS:=$default_java_opts}

# Uncomment below line to enable remote debugging.
#JAVA_OPTS="$JAVA_OPTS -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"

# Uncomment below line to enable remote resource monitoring.
#JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=55555 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=`hostname -I`"

PATH=${JAVA_HOME:-../jre}/bin:$PATH

mkdir -p data/plugins/
mkdir -p data/ovf/
cp -n default-data/plugins/* data/plugins/
cp -n default-data/mainKeyStore.p12 data/
cp -n default-data/osctrustore.jks data/
cp -n default-data/vmidcServer.conf data/

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
