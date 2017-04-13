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

cd `dirname $0`

export JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Xmx64m"
# DEBUG : Uncomment below line for opening debug port.
# /usr/local/sbin/iptables -I INPUT -i eth0 -p tcp --dport 8787 -j ACCEPT
# export JAVA_OPTS="$JAVA_OPTS -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"

../jre/bin/java $JAVA_OPTS -jar vmiDCAgent.jar $*