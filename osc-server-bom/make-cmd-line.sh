#!/bin/bash
source /etc/profile.d/proxy.sh > /dev/null 2>&1
make image-format=${image-format} buildNumber=${buildNumber} -j --no-print-directory -f makefiles/master.mk
