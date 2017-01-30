#!/bin/sh -eu
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
    jre_tgz=$tmp_root/server-jre-8u112-linux-x64.tar.gz
    jre_dir=$my_root/opt/vmidc/jre
    if [ -f "$jre_tgz" ]; then
	tar -xz --directory=$jre_dir -f $jre_tgz --strip 1
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

echo "done upgrading"
exit 0

#end
