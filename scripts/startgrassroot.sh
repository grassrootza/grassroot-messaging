#!/bin/bash
. /etc/environment
. /etc/grassroot

CURR=$PWD
cd /var/grassroot-messaging
nohup java  -Dspring.profiles.active=$PROFILE -jar build/libs/grassroot-messaging-1.0-SNAPSHOT.jar  > grassroot-msg.log 2>&1 &
echo $! > .pid
sleep 1
chgrp sudo /var/grassroot-messaging/grassroot-msg.log
chmod 640 /var/grassroot-messaging/grassroot-msg.log
cd $CURR