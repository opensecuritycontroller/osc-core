#!/bin/sh -eu
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

# BEWARE: -eu exits script if _any_ command fails or variables are unset

# Usage: upgrade.sh major minor build

# This script requires:
# 1. to be part of an upgrade bundle, unpacked to a temporary location.
# 2. working directory is ${my_root}/opt/vmidc/bin

# copy upgrade to /opt
upgrade_copy() {
    chmod +x $tmp_root/opt/vmidc/bin/scripts/*.sh
    chmod +x $tmp_root/opt/vmidc/bin/*.sh
    chmod +x $tmp_root/opt/vmidc/bin/*.py
    cp --preserve --recursive --force $tmp_root/opt/* $my_root/opt
}

# delete obsolete files
upgrade_delete() {
    rm -f lib/trusted-source-2.3.jar
}

# rebrand changes where we have to replace Intel security controller
# with Open security controller
# Since upgrade from ISC to OSC does not change following two files,
# work around or upgrade fix to rebranding changes
upgrade_rebrand() {
    sed -i -e 's/Intel/Open/g' /etc/issue || true
    sed -i -e 's/Intel/Open/g' /etc/init.d/securityBroker || true
}

upgrade_java() {
    jre_tgz=$tmp_root/jre-8u112-linux-x64.tar.gz
    jre_dir=$my_root/opt/vmidc/jre
    if [ -f "$jre_tgz" ]; then
    tar -xz --directory=$jre_dir -f $jre_tgz --strip 1
    fi
}

upgrade_keystore() {
    old_keystore=$my_root/opt/vmidc/bin/vmidcKeyStore.jks
    old_truststore=$my_root/opt/vmidc/bin/vmidctruststore.jks
    new_truststore=$my_root/opt/vmidc/bin/osctrustore.jks
    KEYTOOL=$my_root/opt/vmidc/jre/bin/keytool
    
    if [ ! -f "$new_truststore" ]; then
    cp $old_truststore $new_truststore
    $KEYTOOL -importkeystore -srcstoretype JKS -deststoretype JKS -srckeystore $old_keystore -destkeystore $new_truststore -srcstorepass abc12345 -deststorepass abc12345 -noprompt
    $KEYTOOL -changealias -alias vmidckeystore -destalias internal -keystore $new_truststore -v -storepass abc12345 -noprompt
    fi
}

echo "Start upgrade script"

version_major=${1:-1}
version_minor=${2:-0}
build=${3:-1999}

echo "version major: " $version_major
echo "version minor: " $version_minor
echo "build: " $build

# this script is in /tmp/???/opt/vmidc/bin/scripts
tmp_root=$(CDPATH= cd $(dirname $0)/../../../..; pwd)

# working directory is ${my_root}/opt/vmidc/bin
my_root=$(CDPATH= cd ../../..; pwd)

if [ "$my_root" = "$tmp_root" ]; then
    echo "$0: ERROR: can't run upgrade from current installation." >&2
    exit 2
fi

upgrade_copy
upgrade_delete
upgrade_rebrand
upgrade_java
upgrade_keystore

echo "done upgrading"
exit 0

#end
