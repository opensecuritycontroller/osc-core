#!/bin/sh
# Copyright (c) 2017 Intel Corporation
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

cleanup() {
    cat /spool/cpa/bin/dhclient.pid | xargs kill
    exit
}
trap cleanup INT TERM

/bin/ip link set dev eth0 up

touch /spool/cpa/bin/dhclient.lease
> /spool/cpa/bin/dhclient.lease
/sbin/dhclient -1 -4 -v -lf /spool/cpa/bin/dhclient.lease -pf /spool/cpa/bin/dhclient.pid -sf /spool/cpa/bin/scripts/dhclient-script.sh eth0

cleanup
