#!/bin/bash
. /etc/environment

CURR=$PWD
cd /var/grassroot
./scripts/startmqtt.sh
nohup java  -Dspring.profiles.active=$PROFILE -jar build/libs/grassroot-messaging-1.0-SNAPSHOT.jar  > grassroot-msg.log 2>&1 &
echo $! > .pid
sleep 1
chgrp sudo /var/grassroot/grassroot-app.log
chmod 640 /var/grassroot/grassroot-app.log
cd $CURR