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

echo "Start CPA Upgrade Script"

appliace_type="vnsp"
if [ "$1" != "" ]; then
   appliace_type="$1"
fi
echo "Appliance Type: " $appliace_type

version_major=1
if [ "$2" != "" ]; then
   version_major=$2
fi
echo "CPA Version Major: " $version_major

version_minor=0
if [ "$3" != "" ]; then
   version_minor=$3
fi
echo "CPA Version Minor: " $version_minor

build=1884
if [ "$4" != "" ]; then
   build=$4
fi
echo "CPA Build: " $build

cpadir=`dirname $PWD`
if [ "$5" != "" ]; then
   cpadir=$5
fi
echo "CPA Directory: " $cpadir

if [ "$appliace_type" == "vnsp" ]; then
    cp -p -r -f /tmp/agentUpgradeBundle/vmidc /tftpboot
    cp -p -r -f /tftpboot/vmidc/bin/nsp-appliance-scripts/. /tftpboot/vmidc/bin/scripts

    echo "Giving permission to new files"
    chmod +x /tftpboot/vmidc/bin/vmidc.sh
    chmod +x /tftpboot/vmidc/bin/scripts/*.sh
    chmod +x /tftpboot/vmidc/bin/scripts/*.py

    rm -rf /tftpboot/vmidc/bin/*-scripts

    echo "Persisting CPA files for next reboot"
    install -d /mnt/config/tftpboot/vmidc/bin
    cp -p -f /tftpboot/vmidc/bin/vmidc.sh           /mnt/config/tftpboot/vmidc/bin/.
    cp -p -f /tftpboot/vmidc/bin/vmiDCAgent.jar     /mnt/config/tftpboot/vmidc/bin/.

    echo "Persisting script files for next reboot"
    install -d /mnt/config/tftpboot/vmidc/bin/scripts
    cp -p -f /tftpboot/vmidc/bin/scripts/dpaipc-client.sh       /mnt/config/tftpboot/vmidc/bin/scripts/.

    echo "Listing files for next reboot"
    ls -lR /mnt/config/tftpboot/vmidc/*

elif [ "$appliace_type" == "ngfw" ]; then
    echo "Copying files from '/tmp/agentUpgradeBundle/vmidc/bin/*' to '/spool/cpa/bin/'"
    cp -p -r -f -v /tmp/agentUpgradeBundle/vmidc/bin/* /spool/cpa/bin/
    cp -p -r -f -v /spool/cpa/bin/ngfw-appliance-scripts/. /spool/cpa/bin/scripts/

	echo "Copying files from '/tmp/agentUpgradeBundle/vmidc/jre/*' to '/spool/cpa/jre'"
	cp -p -r -f -v /tmp/agentUpgradeBundle/vmidc/jre/* /spool/cpa/jre/

    echo "Giving permission to new files"
    chmod +x /spool/cpa/bin/vmidc.sh
    chmod +x /spool/cpa/bin/scripts/*.sh
    chmod +x /spool/cpa/bin/scripts/*.py

    rm -rf /spool/cpa/bin/*-scripts

else
    echo "Unknown Appliance Type!. Use generic logic"
	# Dont recursively copy files in the bin folder since we dont want to replace the custom scripts
	echo "Copying files from '/tmp/agentUpgradeBundle/vmidc/bin/*' to '$cpadir/bin'"
    cp -p -f -v /tmp/agentUpgradeBundle/vmidc/bin/* $cpadir/bin
    cp -p -r -f $cpadir/bin/generic-appliance-scripts/. $cpadir/bin/scripts/

	echo "Copying files from '/tmp/agentUpgradeBundle/vmidc/jre/*' to '$cpadir/jre'"
	cp -p -r -f -v /tmp/agentUpgradeBundle/vmidc/jre/* $cpadir/jre

    echo "Giving permission to new files"
    chmod +x $cpadir/bin/vmidc.sh
    chmod +x $cpadir/bin/scripts/*.sh
    chmod +x $cpadir/bin/scripts/*.py

    rm -rf $cpadir/bin/*-scripts
fi

echo "Done CPA upgrading"
