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


cat << EOF
comamnds
  mod <serivce-profile> <pid>
  del <serivce-profile>
  get
  info
  stats
  clearstats
  inspstart
  inspstop
  quit

EOF

mkfifo infifo
trap "rm infifo" EXIT

exec 7>&1
nc 127.0.0.1 10613 < infifo 1>&7 &
exec 7>&-
exec 3> infifo

while read -p "> " REPLY
do
  echo $REPLY | egrep -q '^\W*$' || {
    case $REPLY in
      mod*)
         if echo $REPLY | egrep -q '^mod +[^ ]+ +[0-9]+ *$';  then
           str=`echo $REPLY | sed -r -e 's/mod +([^ ]+) +([0-9]+)/{"cmd":"update-serviceprofile-map","map":[{"service-profile":"\1","policy-id":\2}]}"/'`
           echo $str >&3
           sleep .5
           echo
         else
           echo "Usage: mod <service-profile> <pid>"
         fi
           ;;

      del*)
         if echo $REPLY | egrep -q '^del +[^ ]+ *$';  then
           str=`echo $REPLY | sed -r -e 's/del +([^ ]+)/{"cmd":"update-serviceprofile-map","map":[{"service-profile":"\1"}]}"/'`
           echo $str >&3
           sleep .5
           echo
         else
           echo "Usage: del <service-profile>"
         fi
           ;;

      get*)
        echo '{"cmd":"get-serviceprofile-map"}' >&3
        sleep .5
        echo
           ;;

      stats*)
        echo '{"cmd":"get-statistics"}' >&3
        sleep .5
        echo
           ;;

      clearstats*)
        echo '{"cmd":"clear-statistics"}' >&3
        sleep .5
        echo
           ;;

      inspstart*)
        echo '{"cmd":"inspection-start"}' >&3
        sleep .5
        echo
           ;;

      inspstop*)
        echo '{"cmd":"inspection-stop"}' >&3
        sleep .5
        echo
           ;;

      info*)
        echo '{"cmd":"dpa-info"}' >&3
        sleep .5
        echo
           ;;

      quit*)
         exit
           ;;

      *)
         echo Unknown command
           ;;
    esac
  }
done
